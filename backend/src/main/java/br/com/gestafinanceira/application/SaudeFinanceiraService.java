package br.com.gestafinanceira.application;

import br.com.gestafinanceira.api.dto.Dtos.CategoriaResumoDto;
import br.com.gestafinanceira.api.dto.Dtos.CategoriaSaudeDto;
import br.com.gestafinanceira.api.dto.Dtos.PoupancaDetalhe;
import br.com.gestafinanceira.api.dto.Dtos.SaudeFinanceiraDto;
import br.com.gestafinanceira.domain.calculo.Dinheiro;
import br.com.gestafinanceira.domain.calculo.projecao.MesProjecao;
import br.com.gestafinanceira.domain.calculo.projecao.ProjetorMensal;
import br.com.gestafinanceira.api.dto.Dtos.ProjecaoDto;
import br.com.gestafinanceira.domain.calculo.saude.Metodo503020;
import br.com.gestafinanceira.domain.calculo.saude.SaudeAdicionais;
import br.com.gestafinanceira.domain.calculo.saude.Metodo503020.Resultado;
import br.com.gestafinanceira.domain.modelo.GrupoGasto;
import br.com.gestafinanceira.infra.persistencia.CategoriaRepo;
import br.com.gestafinanceira.infra.persistencia.Entidades.Categoria;
import br.com.gestafinanceira.infra.persistencia.Entidades.Perfil;
import br.com.gestafinanceira.infra.persistencia.PerfilRepo;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Saúde financeira (método 50/30/20). Sempre calcula, mesmo sem participação (a flag só registra a adesão).
 * Renda = líquido do "mês comum" do mês atual + contribuição FUNCEF do participante (que conta como poupança;
 * a contrapartida da patrocinadora não entra, pois não é renda do usuário); gastos = médias mensais do resumo de lançamentos somadas por
 * grupo da categoria; poupança = média mensal dos aportes em metas nos 12 meses da projeção + FUNCEF do mês comum.
 */
@Service
@Transactional
public class SaudeFinanceiraService {
    private final PerfilService perfil;
    private final PerfilRepo perfis;
    private final ProjecaoService projecao;
    private final LancamentoService lancamentos;
    private final CategoriaRepo categorias;

    public SaudeFinanceiraService(PerfilService perfil, PerfilRepo perfis, ProjecaoService projecao,
                                  LancamentoService lancamentos, CategoriaRepo categorias) {
        this.perfil = perfil;
        this.perfis = perfis;
        this.projecao = projecao;
        this.lancamentos = lancamentos;
        this.categorias = categorias;
    }

    public SaudeFinanceiraDto buscar() {
        Perfil p = perfil.entidade();
        // A contribuição FUNCEF sai do líquido e volta como poupança: a renda de referência a soma de volta.
        MesProjecao comum = projecao.mesComum(null);
        BigDecimal funcef = comum.funcef();
        BigDecimal renda = comum.liquido().add(funcef);

        Map<String, BigDecimal> medias = new HashMap<>();
        for (CategoriaResumoDto c : lancamentos.resumoPorCategoria(null).categorias()) {
            medias.merge(c.categoria(), c.mediaMensal(), BigDecimal::add);
        }
        List<Categoria> todas = categorias.findAllByOrderByOrdemAscNomeAsc();
        Map<String, GrupoGasto> grupos = new HashMap<>();
        List<CategoriaSaudeDto> lista = new ArrayList<>();
        BigDecimal necessidades = BigDecimal.ZERO;
        BigDecimal desejos = BigDecimal.ZERO;
        for (Categoria c : todas) {
            grupos.put(c.nome, c.grupo);
            BigDecimal media = Dinheiro.arredondar(medias.getOrDefault(c.nome, BigDecimal.ZERO));
            lista.add(new CategoriaSaudeDto(c.nome, c.grupo, media));
            if (c.grupo == GrupoGasto.DESEJO) desejos = desejos.add(media);
            else necessidades = necessidades.add(media);
        }
        // categoria sem cadastro (não deveria ocorrer): conta como necessidade
        for (Map.Entry<String, BigDecimal> e : medias.entrySet()) {
            if (!grupos.containsKey(e.getKey())) necessidades = necessidades.add(e.getValue());
        }

        ProjecaoDto proj = projecao.projetar(null);
        BigDecimal aportes = proj.resumo().aportes();
        BigDecimal poupanca = Dinheiro.arredondar(aportes.divide(BigDecimal.valueOf(ProjetorMensal.MESES), 10,
                RoundingMode.HALF_UP));

        Resultado r = Metodo503020.calcular(renda, necessidades, desejos, poupanca, funcef);
        // Bloco à parte: 13º e PLR do período (não entram no 50/30/20 mensal, que usa a renda de mês comum)
        SaudeAdicionais.Resultado adicionais = SaudeAdicionais.calcular(
                proj.meses().stream().flatMap(m -> m.adicionais().stream()).toList());
        List<String> diagnostico = new ArrayList<>(r.diagnostico());
        diagnostico.addAll(adicionais.diagnostico());
        return new SaudeFinanceiraDto(p.metodo503020, r.rendaMensal(), r.recomendado(), r.atual(), r.percentuais(),
                r.diferenca(), r.status(), r.sobraLivre(), r.aporteSugerido(), lista, diagnostico,
                new PoupancaDetalhe(r.poupancaDetalhe().metas(), r.poupancaDetalhe().funcef()), adicionais);
    }

    public SaudeFinanceiraDto definirParticipacao(boolean participa) {
        Perfil p = perfil.entidade();
        p.metodo503020 = participa;
        p.atualizadoEm = LocalDateTime.now();
        perfis.save(p);
        return buscar();
    }
}
