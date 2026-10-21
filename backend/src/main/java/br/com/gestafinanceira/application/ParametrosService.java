package br.com.gestafinanceira.application;

import br.com.gestafinanceira.api.dto.Dtos.BeneficioDto;
import br.com.gestafinanceira.api.dto.Dtos.CargoDto;
import br.com.gestafinanceira.api.dto.Dtos.DeltaDto;
import br.com.gestafinanceira.api.dto.Dtos.FaixaDto;
import br.com.gestafinanceira.api.dto.Dtos.ParametrosVigentesDto;
import br.com.gestafinanceira.api.dto.Dtos.TabelaDeltasDto;
import br.com.gestafinanceira.api.dto.Dtos.TabelaFaixasDto;
import br.com.gestafinanceira.application.Erros.NaoEncontrado;
import br.com.gestafinanceira.application.Erros.RegraDeNegocio;
import br.com.gestafinanceira.domain.calculo.CargoVigente;
import br.com.gestafinanceira.domain.calculo.FaixaProgressiva;
import br.com.gestafinanceira.domain.calculo.Parametros;
import br.com.gestafinanceira.domain.calculo.ParametrosMes;
import br.com.gestafinanceira.infra.persistencia.Entidades;
import br.com.gestafinanceira.infra.persistencia.Entidades.Cargo;
import br.com.gestafinanceira.infra.persistencia.Entidades.CargoValor;
import br.com.gestafinanceira.infra.persistencia.Entidades.DeltaSalarial;
import br.com.gestafinanceira.infra.persistencia.Entidades.InssFaixa;
import br.com.gestafinanceira.infra.persistencia.Entidades.IrpfFaixa;
import br.com.gestafinanceira.infra.persistencia.Entidades.ParametroBeneficio;
import br.com.gestafinanceira.infra.persistencia.BeneficioRepo;
import br.com.gestafinanceira.infra.persistencia.CargoRepo;
import br.com.gestafinanceira.infra.persistencia.CargoValorRepo;
import br.com.gestafinanceira.infra.persistencia.DeltaRepo;
import br.com.gestafinanceira.infra.persistencia.InssRepo;
import br.com.gestafinanceira.infra.persistencia.IrpfRepo;
import br.com.gestafinanceira.infra.persistencia.PlrRepo;
import br.com.gestafinanceira.infra.persistencia.PerfilRepo;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tabelas e valores com vigência. Para um mês, vale o conjunto cuja vigência inicial é a mais recente
 * até o dia 1º do mês; se o mês for anterior a todas, vale o conjunto mais antigo.
 */
@Service
@Transactional
public class ParametrosService {
    private final CargoRepo cargos;
    private final CargoValorRepo cargoValores;
    private final DeltaRepo deltas;
    private final InssRepo inss;
    private final IrpfRepo irpf;
    private final PlrRepo plrFaixas;
    private final BeneficioRepo beneficios;
    private final PerfilRepo perfil;

    public ParametrosService(CargoRepo cargos, CargoValorRepo cargoValores, DeltaRepo deltas, InssRepo inss,
                             IrpfRepo irpf, PlrRepo plrFaixas, BeneficioRepo beneficios, PerfilRepo perfil) {
        this.plrFaixas = plrFaixas;
        this.cargos = cargos;
        this.cargoValores = cargoValores;
        this.deltas = deltas;
        this.inss = inss;
        this.irpf = irpf;
        this.beneficios = beneficios;
        this.perfil = perfil;
    }

    // ---- leitura para o cálculo ---------------------------------------------------------------

    @Transactional(readOnly = true)
    public ParametrosMes paraMes(YearMonth mes, Long cargoId, int deltaRef) {
        LocalDate data = mes.atDay(1);
        ParametroBeneficio b = beneficios.vigente(data).or(beneficios::maisAntigo)
                .orElseThrow(() -> new RegraDeNegocio("Não há parâmetros de benefícios cadastrados."));
        List<InssFaixa> fi = inss.vigentes(data);
        if (fi.isEmpty()) fi = inss.maisAntigas();
        List<IrpfFaixa> fr = irpf.vigentes(data);
        if (fr.isEmpty()) fr = irpf.maisAntigas();
        if (fi.isEmpty() || fr.isEmpty()) throw new RegraDeNegocio("Tabelas de INSS/IRPF não cadastradas.");

        Parametros p = new Parametros(b.pai, b.homeOffice, b.vrVa, b.vtPercentual,
                b.sindicatoPercentual, b.sindicatoTeto, b.saudePercentual, b.saudeTetoPercentual,
                b.planoPorDependente, b.inssTeto, b.inssTetoSalario, b.irDeducaoMinima, b.irLimiteIsencao,
                b.irLimiteReducao, b.irReducaoA, b.irReducaoB, b.divisorJornada6h, b.divisorJornada8h,
                b.bonusCaixaMaxSalarios,
                fi.stream().map(f -> new FaixaProgressiva(f.ate, f.aliquota, f.deducao)).toList(),
                fr.stream().map(f -> new FaixaProgressiva(f.ate, f.aliquota, f.deducao)).toList(),
                faixasPlr(data));

        Cargo c = cargos.findById(cargoId).orElseThrow(() -> new NaoEncontrado("Cargo " + cargoId + " não encontrado."));
        CargoValor v = cargoValores.vigente(cargoId, data).or(() -> cargoValores.maisAntigo(cargoId))
                .orElseThrow(() -> new RegraDeNegocio("Cargo '" + c.nome + "' sem valores cadastrados."));
        // O valor do delta também alimenta o VT (4% do delta do perfil), mesmo em cargo que não usa delta na base.
        var delta = deltas.vigente(deltaRef, data).or(() -> deltas.maisAntigo(deltaRef));
        if (c.usaDelta && delta.isEmpty()) {
            throw new RegraDeNegocio("Delta " + deltaRef + " não existe na tabela de deltas.");
        }
        BigDecimal valorDelta = delta.map(d -> d.valor).orElse(BigDecimal.ZERO);
        return new ParametrosMes(p, new CargoVigente(c.nome, c.jornada, c.usaDelta, v.piso, v.gratificacao,
                v.plrAnual != null ? v.plrAnual : BigDecimal.ZERO), valorDelta);
    }

    /** Tabela de IR exclusivo do PLR vigente na data (a mais antiga se todas começam depois; vazia se não há). */
    @Transactional(readOnly = true)
    public List<FaixaProgressiva> faixasPlr(LocalDate data) {
        List<Entidades.PlrFaixa> f = plrFaixas.vigentes(data);
        if (f.isEmpty()) f = plrFaixas.maisAntigas();
        return f.stream().map(x -> new FaixaProgressiva(x.ate, x.aliquota, x.deducao)).toList();
    }

    @Transactional(readOnly = true)
    public boolean deltaExiste(int referencia) {
        return deltas.maisAntigo(referencia).isPresent();
    }

    /** Foto dos parâmetros vigentes na data (padrão: hoje). */
    @Transactional(readOnly = true)
    public ParametrosVigentesDto vigentes(LocalDate data) {
        LocalDate d = data != null ? data : LocalDate.now();
        return new ParametrosVigentesDto(d, listarCargos(d),
                escolher(listarDeltas(), TabelaDeltasDto::vigenciaInicio, d),
                escolher(listarInss(), TabelaFaixasDto::vigenciaInicio, d),
                escolher(listarIrpf(), TabelaFaixasDto::vigenciaInicio, d),
                escolher(listarBeneficios(), BeneficioDto::vigenciaInicio, d));
    }

    /** Vigência mais recente iniciada até a data; se todas começam depois, a mais antiga. */
    private static <T> T escolher(List<T> vigencias, Function<T, LocalDate> inicio, LocalDate data) {
        return vigencias.stream().filter(v -> !inicio.apply(v).isAfter(data))
                .max(Comparator.comparing(inicio))
                .orElseGet(() -> vigencias.stream().min(Comparator.comparing(inicio))
                        .orElseThrow(() -> new RegraDeNegocio("Parâmetros não cadastrados.")));
    }

    // ---- cargos -------------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<CargoDto> listarCargos(LocalDate data) {
        LocalDate d = data != null ? data : LocalDate.now();
        return cargos.findAllByOrderByOrdemAscIdAsc().stream()
                .map(c -> paraDto(c, cargoValores.vigente(c.id, d).or(() -> cargoValores.maisAntigo(c.id)).orElse(null)))
                .toList();
    }

    @Transactional(readOnly = true)
    public CargoDto buscarCargo(Long id) {
        Cargo c = cargos.findById(id).orElseThrow(() -> new NaoEncontrado("Cargo " + id + " não encontrado."));
        return paraDto(c, cargoValores.vigente(id, LocalDate.now()).or(() -> cargoValores.maisAntigo(id)).orElse(null));
    }

    public CargoDto criarCargo(CargoDto dto) {
        if (cargos.existsByNomeIgnoreCase(dto.nome())) throw new RegraDeNegocio("Já existe um cargo com esse nome.");
        Cargo c = new Cargo();
        aplicar(c, dto);
        cargos.save(c);
        salvarValor(c.id, dto);
        return buscarCargo(c.id);
    }

    public CargoDto atualizarCargo(Long id, CargoDto dto) {
        Cargo c = cargos.findById(id).orElseThrow(() -> new NaoEncontrado("Cargo " + id + " não encontrado."));
        if (!c.nome.equalsIgnoreCase(dto.nome()) && cargos.existsByNomeIgnoreCase(dto.nome())) {
            throw new RegraDeNegocio("Já existe um cargo com esse nome.");
        }
        aplicar(c, dto);
        salvarValor(id, dto);
        return buscarCargo(id);
    }

    public void removerCargo(Long id) {
        if (!cargos.existsById(id)) throw new NaoEncontrado("Cargo " + id + " não encontrado.");
        if (perfil.findAll().stream().anyMatch(p -> id.equals(p.cargoId))) {
            throw new RegraDeNegocio("O cargo está em uso no perfil; troque o cargo do perfil antes de removê-lo.");
        }
        cargoValores.deleteByCargoId(id);
        cargos.deleteById(id);
    }

    private void aplicar(Cargo c, CargoDto dto) {
        c.nome = dto.nome().trim();
        c.jornada = (short) dto.jornada();
        c.usaDelta = dto.usaDelta();
        c.ordem = dto.ordem();
    }

    private void salvarValor(Long cargoId, CargoDto dto) {
        LocalDate inicio = dto.vigenciaInicio() != null ? dto.vigenciaInicio() : LocalDate.now().withDayOfMonth(1);
        CargoValor v = cargoValores.findByCargoIdAndVigenciaInicio(cargoId, inicio).orElseGet(CargoValor::new);
        v.cargoId = cargoId;
        v.vigenciaInicio = inicio;
        v.piso = dto.piso();
        v.gratificacao = dto.gratificacao();
        if (dto.plrAnual() != null) v.plrAnual = dto.plrAnual();
        cargoValores.save(v);
        cargoValores.flush();
        normalizarFim(cargoValores.findByCargoIdOrderByVigenciaInicioAsc(cargoId), x -> x.vigenciaInicio,
                (x, fim) -> x.vigenciaFim = fim);
    }

    private CargoDto paraDto(Cargo c, CargoValor v) {
        return new CargoDto(c.id, c.nome, c.jornada, c.usaDelta, c.ordem,
                v != null ? v.piso : BigDecimal.ZERO, v != null ? v.gratificacao : BigDecimal.ZERO,
                v != null ? v.vigenciaInicio : null, v != null ? v.plrAnual : BigDecimal.ZERO);
    }

    // ---- INSS / IRPF --------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<TabelaFaixasDto> listarInss() {
        return agrupar(inss.findAllByOrderByVigenciaInicioAscAteAsc(), f -> f.vigenciaInicio, f -> f.vigenciaFim,
                f -> new FaixaDto(f.ate, f.aliquota, f.deducao), TabelaFaixasDto::new);
    }

    public List<TabelaFaixasDto> salvarInss(TabelaFaixasDto dto) {
        validarFaixas(dto.faixas(), false);
        inss.deleteByVigenciaInicio(dto.vigenciaInicio());
        inss.flush();
        for (FaixaDto f : ordenar(dto.faixas())) {
            InssFaixa e = new InssFaixa();
            e.ate = f.ate();
            e.aliquota = f.aliquota();
            e.deducao = f.deducao();
            e.vigenciaInicio = dto.vigenciaInicio();
            inss.save(e);
        }
        inss.flush();
        normalizarFim(inss.findAll(), f -> f.vigenciaInicio, (f, fim) -> f.vigenciaFim = fim);
        return listarInss();
    }

    public List<TabelaFaixasDto> removerInss(LocalDate inicio) {
        if (inss.findAll().stream().noneMatch(f -> f.vigenciaInicio.equals(inicio))) {
            throw new NaoEncontrado("Vigência de INSS " + inicio + " não encontrada.");
        }
        if (inss.findAll().stream().map(f -> f.vigenciaInicio).distinct().count() <= 1) {
            throw new RegraDeNegocio("Não é possível remover a única vigência da tabela de INSS.");
        }
        inss.deleteByVigenciaInicio(inicio);
        inss.flush();
        normalizarFim(inss.findAll(), f -> f.vigenciaInicio, (f, fim) -> f.vigenciaFim = fim);
        return listarInss();
    }

    @Transactional(readOnly = true)
    public List<TabelaFaixasDto> listarIrpf() {
        return agrupar(irpf.findAllByOrderByVigenciaInicioAscIdAsc(), f -> f.vigenciaInicio, f -> f.vigenciaFim,
                f -> new FaixaDto(f.ate, f.aliquota, f.deducao), TabelaFaixasDto::new);
    }

    public List<TabelaFaixasDto> salvarIrpf(TabelaFaixasDto dto) {
        validarFaixas(dto.faixas(), true);
        irpf.deleteByVigenciaInicio(dto.vigenciaInicio());
        irpf.flush();
        for (FaixaDto f : ordenar(dto.faixas())) {
            IrpfFaixa e = new IrpfFaixa();
            e.ate = f.ate();
            e.aliquota = f.aliquota();
            e.deducao = f.deducao();
            e.vigenciaInicio = dto.vigenciaInicio();
            irpf.save(e);
        }
        irpf.flush();
        normalizarFim(irpf.findAll(), f -> f.vigenciaInicio, (f, fim) -> f.vigenciaFim = fim);
        return listarIrpf();
    }

    public List<TabelaFaixasDto> removerIrpf(LocalDate inicio) {
        if (irpf.findAll().stream().noneMatch(f -> f.vigenciaInicio.equals(inicio))) {
            throw new NaoEncontrado("Vigência de IRPF " + inicio + " não encontrada.");
        }
        if (irpf.findAll().stream().map(f -> f.vigenciaInicio).distinct().count() <= 1) {
            throw new RegraDeNegocio("Não é possível remover a única vigência da tabela de IRPF.");
        }
        irpf.deleteByVigenciaInicio(inicio);
        irpf.flush();
        normalizarFim(irpf.findAll(), f -> f.vigenciaInicio, (f, fim) -> f.vigenciaFim = fim);
        return listarIrpf();
    }

    private static List<FaixaDto> ordenar(List<FaixaDto> faixas) {
        return faixas.stream()
                .sorted(Comparator.comparing(FaixaDto::ate, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private static void validarFaixas(List<FaixaDto> faixas, boolean ultimaIlimitada) {
        long semLimite = faixas.stream().filter(f -> f.ate() == null).count();
        if (semLimite > 1 || (!ultimaIlimitada && semLimite > 0)) {
            throw new RegraDeNegocio(ultimaIlimitada
                    ? "Somente a última faixa pode ficar sem limite superior."
                    : "Todas as faixas do INSS precisam de limite superior.");
        }
        long distintos = faixas.stream().map(FaixaDto::ate).distinct().count();
        if (distintos != faixas.size()) throw new RegraDeNegocio("Há faixas com o mesmo limite superior.");
    }

    // ---- deltas -------------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<TabelaDeltasDto> listarDeltas() {
        Map<LocalDate, List<DeltaSalarial>> porInicio = new LinkedHashMap<>();
        for (DeltaSalarial d : deltas.findAllByOrderByVigenciaInicioAscReferenciaAsc()) {
            porInicio.computeIfAbsent(d.vigenciaInicio, k -> new ArrayList<>()).add(d);
        }
        return porInicio.entrySet().stream()
                .map(e -> new TabelaDeltasDto(e.getKey(), e.getValue().get(0).vigenciaFim,
                        e.getValue().stream().map(d -> new DeltaDto(d.referencia, d.valor)).toList()))
                .toList();
    }

    public List<TabelaDeltasDto> salvarDeltas(TabelaDeltasDto dto) {
        if (dto.deltas().stream().map(DeltaDto::referencia).distinct().count() != dto.deltas().size()) {
            throw new RegraDeNegocio("Há referências de delta repetidas.");
        }
        deltas.deleteByVigenciaInicio(dto.vigenciaInicio());
        deltas.flush();
        for (DeltaDto d : dto.deltas()) {
            DeltaSalarial e = new DeltaSalarial();
            e.referencia = d.referencia();
            e.valor = d.valor();
            e.vigenciaInicio = dto.vigenciaInicio();
            deltas.save(e);
        }
        deltas.flush();
        normalizarFim(deltas.findAll(), d -> d.vigenciaInicio, (d, fim) -> d.vigenciaFim = fim);
        return listarDeltas();
    }

    public List<TabelaDeltasDto> removerDeltas(LocalDate inicio) {
        List<DeltaSalarial> todos = deltas.findAll();
        if (todos.stream().noneMatch(d -> d.vigenciaInicio.equals(inicio))) {
            throw new NaoEncontrado("Vigência de deltas " + inicio + " não encontrada.");
        }
        if (todos.stream().map(d -> d.vigenciaInicio).distinct().count() <= 1) {
            throw new RegraDeNegocio("Não é possível remover a única vigência da tabela de deltas.");
        }
        deltas.deleteByVigenciaInicio(inicio);
        deltas.flush();
        normalizarFim(deltas.findAll(), d -> d.vigenciaInicio, (d, fim) -> d.vigenciaFim = fim);
        return listarDeltas();
    }

    // ---- benefícios ---------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<BeneficioDto> listarBeneficios() {
        return beneficios.findAllByOrderByVigenciaInicioAsc().stream().map(ParametrosService::paraDto).toList();
    }

    public List<BeneficioDto> salvarBeneficio(BeneficioDto d) {
        ParametroBeneficio b = beneficios.findByVigenciaInicio(d.vigenciaInicio()).orElseGet(ParametroBeneficio::new);
        b.vigenciaInicio = d.vigenciaInicio();
        b.pai = d.pai();
        b.homeOffice = d.homeOffice();
        b.vrVa = d.vrVa();
        b.vtPercentual = d.vtPercentual();
        b.sindicatoPercentual = d.sindicatoPercentual();
        b.sindicatoTeto = d.sindicatoTeto();
        b.saudePercentual = d.saudePercentual();
        b.saudeTetoPercentual = d.saudeTetoPercentual();
        b.planoPorDependente = d.planoPorDependente();
        b.inssTeto = d.inssTeto();
        b.inssTetoSalario = d.inssTetoSalario();
        b.irDeducaoMinima = d.irDeducaoMinima();
        b.irLimiteIsencao = d.irLimiteIsencao();
        b.irLimiteReducao = d.irLimiteReducao();
        b.irReducaoA = d.irReducaoA();
        b.irReducaoB = d.irReducaoB();
        b.divisorJornada6h = d.divisorJornada6h();
        b.divisorJornada8h = d.divisorJornada8h();
        b.bonusCaixaMaxSalarios = d.bonusCaixaMaxSalarios();
        beneficios.save(b);
        beneficios.flush();
        normalizarFim(beneficios.findAllByOrderByVigenciaInicioAsc(), x -> x.vigenciaInicio, (x, fim) -> x.vigenciaFim = fim);
        return listarBeneficios();
    }

    public List<BeneficioDto> removerBeneficio(LocalDate inicio) {
        ParametroBeneficio b = beneficios.findByVigenciaInicio(inicio)
                .orElseThrow(() -> new NaoEncontrado("Vigência de benefícios " + inicio + " não encontrada."));
        if (beneficios.count() <= 1) throw new RegraDeNegocio("Não é possível remover a única vigência de benefícios.");
        beneficios.delete(b);
        beneficios.flush();
        normalizarFim(beneficios.findAllByOrderByVigenciaInicioAsc(), x -> x.vigenciaInicio, (x, fim) -> x.vigenciaFim = fim);
        return listarBeneficios();
    }

    private static BeneficioDto paraDto(ParametroBeneficio b) {
        return new BeneficioDto(b.vigenciaInicio, b.vigenciaFim, b.pai, b.homeOffice, b.vrVa,
                b.vtPercentual, b.sindicatoPercentual, b.sindicatoTeto, b.saudePercentual, b.saudeTetoPercentual,
                b.planoPorDependente, b.inssTeto, b.inssTetoSalario, b.irDeducaoMinima, b.irLimiteIsencao,
                b.irLimiteReducao, b.irReducaoA, b.irReducaoB, b.divisorJornada6h, b.divisorJornada8h,
                b.bonusCaixaMaxSalarios);
    }

    /** Máximo de salários do Bônus Caixa na vigência de hoje (ou a mais antiga, se todas começam depois). */
    @Transactional(readOnly = true)
    public BigDecimal bonusMaxSalarios() {
        return beneficios.vigente(LocalDate.now()).or(beneficios::maisAntigo)
                .map(b -> b.bonusCaixaMaxSalarios)
                .orElseThrow(() -> new RegraDeNegocio("Não há parâmetros de benefícios cadastrados."));
    }

    // ---- utilitários --------------------------------------------------------------------------

    /** Ajusta vigencia_fim de cada vigência para o dia anterior ao início da seguinte (a última fica aberta). */
    private static <E> void normalizarFim(List<E> linhas, Function<E, LocalDate> inicio, BiConsumer<E, LocalDate> setFim) {
        List<LocalDate> inicios = linhas.stream().map(inicio).distinct().sorted().toList();
        for (E linha : linhas) {
            int i = inicios.indexOf(inicio.apply(linha));
            setFim.accept(linha, i + 1 < inicios.size() ? inicios.get(i + 1).minusDays(1) : null);
        }
    }

    private static <E, D, T> List<T> agrupar(List<E> linhas, Function<E, LocalDate> inicio, Function<E, LocalDate> fim,
                                             Function<E, D> item, TriFunction<LocalDate, LocalDate, List<D>, T> montar) {
        Map<LocalDate, List<E>> porInicio = new LinkedHashMap<>();
        for (E l : linhas) porInicio.computeIfAbsent(inicio.apply(l), k -> new ArrayList<>()).add(l);
        return porInicio.entrySet().stream()
                .map(e -> montar.apply(e.getKey(), fim.apply(e.getValue().get(0)),
                        e.getValue().stream().map(item).toList()))
                .toList();
    }

    @FunctionalInterface
    private interface TriFunction<A, B, C, R> {
        R apply(A a, B b, C c);
    }
}
