package br.com.gestafinanceira.application;

import br.com.gestafinanceira.api.dto.Dtos.BonusDto;
import br.com.gestafinanceira.api.dto.Dtos.FeriasDto;
import br.com.gestafinanceira.api.dto.Dtos.PerfilDto;
import br.com.gestafinanceira.application.Erros.NaoEncontrado;
import br.com.gestafinanceira.application.Erros.RegraDeNegocio;
import br.com.gestafinanceira.domain.modelo.Saude;
import br.com.gestafinanceira.infra.persistencia.Entidades.Cargo;
import br.com.gestafinanceira.infra.persistencia.Entidades.Perfil;
import br.com.gestafinanceira.infra.persistencia.CargoRepo;
import br.com.gestafinanceira.infra.persistencia.PerfilRepo;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PerfilService {
    private static final short ID = 1;
    private static final BigDecimal FUNCEF_MIN = BigDecimal.valueOf(5);
    private static final BigDecimal FUNCEF_MAX = BigDecimal.valueOf(12);

    private final PerfilRepo perfis;
    private final CargoRepo cargos;
    private final ParametrosService parametros;

    public PerfilService(PerfilRepo perfis, CargoRepo cargos, ParametrosService parametros) {
        this.perfis = perfis;
        this.cargos = cargos;
        this.parametros = parametros;
    }

    @Transactional(readOnly = true)
    public Perfil entidade() {
        return perfis.findById(ID).orElseThrow(() -> new NaoEncontrado("Perfil não inicializado."));
    }

    @Transactional(readOnly = true)
    public PerfilDto buscar() {
        Perfil p = entidade();
        return new PerfilDto(p.cargoId, p.delta, p.diasSobreaviso, p.filhos, p.homeOffice, p.vrVa, p.saude,
                p.dependentes, p.funcefPct, p.valeTransporte, p.sindicato, p.saldoInicial, p.nome, p.saldoFuncef);
    }

    public PerfilDto atualizar(PerfilDto d) {
        BigDecimal funcef = d.funcefPct();
        boolean funcefValida = funcef.signum() == 0
                || (funcef.compareTo(FUNCEF_MIN) >= 0 && funcef.compareTo(FUNCEF_MAX) <= 0);
        if (!funcefValida) {
            throw new RegraDeNegocio("FUNCEF: informe 0 (não participa) ou um percentual entre 5% e 12%.");
        }
        Cargo cargo = cargos.findById(d.cargoId())
                .orElseThrow(() -> new RegraDeNegocio("Cargo " + d.cargoId() + " não existe."));
        if (cargo.usaDelta && !parametros.deltaExiste(d.delta())) {
            throw new RegraDeNegocio("Delta " + d.delta() + " não existe na tabela de deltas.");
        }
        Perfil p = perfis.findById(ID).orElseGet(Perfil::new);
        if (p.bonusAtivo && cargo.jornada != 8) {
            throw new RegraDeNegocio("O cargo '" + cargo.nome + "' é de jornada de 6h e não recebe Bônus Caixa; "
                    + "desative o bônus antes de trocar de cargo.");
        }
        p.id = ID;
        p.cargoId = d.cargoId();
        p.delta = d.delta();
        p.diasSobreaviso = d.diasSobreaviso();
        p.filhos = d.filhos();
        p.homeOffice = d.homeOffice();
        p.vrVa = d.vrVa();
        p.saude = d.saude();
        p.dependentes = d.dependentes();
        p.funcefPct = d.funcefPct();
        p.valeTransporte = d.valeTransporte();
        p.sindicato = d.sindicato();
        p.saldoInicial = d.saldoInicial();
        // saldoFuncef ausente (null) mantém o atual
        if (d.saldoFuncef() != null) {
            p.saldoFuncef = d.saldoFuncef();
        }
        // nome ausente (null) mantém o atual; vazio/branco limpa para null
        if (d.nome() != null) {
            p.nome = d.nome().isBlank() ? null : d.nome();
        }
        p.atualizadoEm = LocalDateTime.now();
        perfis.save(p);
        return buscar();
    }

    @Transactional(readOnly = true)
    public FeriasDto buscarFerias() {
        Perfil p = entidade();
        return new FeriasDto(p.feriasAtivo, p.feriasMes, p.feriasAbono);
    }

    public FeriasDto atualizarFerias(FeriasDto d) {
        Perfil p = entidade();
        p.feriasAtivo = d.ativo();
        p.feriasMes = (short) d.mes();
        p.feriasAbono = d.abono();
        p.atualizadoEm = LocalDateTime.now();
        perfis.save(p);
        return buscarFerias();
    }

    @Transactional(readOnly = true)
    public BonusDto buscarBonus() {
        Perfil p = entidade();
        return new BonusDto(p.bonusAtivo, p.bonusMes, p.bonusSalarios);
    }

    /** Bônus Caixa: só cargos de jornada de 8h; salários ≤ máximo vigente. */
    public BonusDto atualizarBonus(BonusDto d) {
        Perfil p = entidade();
        BigDecimal max = parametros.bonusMaxSalarios();
        if (d.salarios().compareTo(max) > 0) {
            throw new RegraDeNegocio("O Bônus Caixa admite no máximo " + max.stripTrailingZeros().toPlainString()
                    + " salários.");
        }
        if (d.ativo()) {
            Cargo cargo = cargos.findById(p.cargoId)
                    .orElseThrow(() -> new RegraDeNegocio("Cargo " + p.cargoId + " não existe."));
            if (cargo.jornada != 8) {
                throw new RegraDeNegocio("O Bônus Caixa só existe para cargos de jornada de 8h; o cargo '"
                        + cargo.nome + "' é de 6h.");
            }
        }
        p.bonusAtivo = d.ativo();
        p.bonusMes = (short) d.mes();
        p.bonusSalarios = d.salarios();
        p.atualizadoEm = LocalDateTime.now();
        perfis.save(p);
        return buscarBonus();
    }

    /** Perfil padrão (equivale a dadosIniciais() do frontend), usado em "restaurar padrão". Mantém o nome do usuário. */
    public void restaurarPadrao() {
        Perfil p = perfis.findById(ID).orElseGet(Perfil::new);
        Cargo cargo = cargos.findAllByOrderByOrdemAscIdAsc().stream().findFirst()
                .orElseThrow(() -> new RegraDeNegocio("Nenhum cargo cadastrado."));
        p.id = ID;
        p.cargoId = cargo.id;
        p.delta = 228;
        p.diasSobreaviso = 0;
        p.filhos = 1;
        p.homeOffice = true;
        p.vrVa = false;
        p.saude = Saude.DEP;
        p.dependentes = 2;
        p.funcefPct = BigDecimal.valueOf(6);
        p.valeTransporte = true;
        p.sindicato = true;
        p.saldoInicial = BigDecimal.valueOf(3200);
        p.saldoFuncef = BigDecimal.ZERO;
        p.feriasAtivo = true;
        p.feriasMes = 7;
        p.feriasAbono = false;
        p.bonusAtivo = false;
        p.bonusMes = 12;
        p.bonusSalarios = BigDecimal.ZERO;
        p.metodo503020 = false;
        p.atualizadoEm = LocalDateTime.now();
        perfis.save(p);
    }
}
