package br.com.techmind.classificador.controller;

import br.com.techmind.classificador.dto.AtualizacaoArtigoRequest;
import br.com.techmind.classificador.dto.ClassificacaoRequest;
import br.com.techmind.classificador.dto.ClassificacaoResponse;
import br.com.techmind.classificador.dto.EstatisticasResponse;
import br.com.techmind.classificador.dto.FeedbackResponse;
import br.com.techmind.classificador.dto.ModeracaoRequest;
import br.com.techmind.classificador.dto.PaginaArtigosResponse;
import br.com.techmind.classificador.service.ArtigoService;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;

import java.util.Map;
import br.com.techmind.classificador.dto.ArtigoResponse;

/**
 * @author Diego Pitoco
 */
@RestController
@RequestMapping("/api/artigos")
@Tag(name = "Artigos", description = "Classificação automática de artigos técnicos")
public class ArtigoController {
    private final ArtigoService artigoService;

    public ArtigoController(ArtigoService artigoService) { this.artigoService = artigoService; }

    @PostMapping("/classificar")
    @Operation(summary = "Classifica um artigo", description = "Combina título e texto e consulta o classificador de Machine Learning.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Artigo classificado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Título ou texto inválido"),
            @ApiResponse(responseCode = "502", description = "Serviço de ML indisponível")
    })
    public ResponseEntity<ClassificacaoResponse> classificar(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(
                            value = "{\"titulo\":\"Construindo APIs REST com Spring Boot\",\"texto\":\"Artigo sobre Java, Spring Boot e Docker\",\"autores\":\"Equipe TechMind\",\"link\":\"https://exemplo.com/artigo\",\"ano\":2026}")))
            @Valid @RequestBody ClassificacaoRequest request) {
        return ResponseEntity.ok(artigoService.classificar(request));
    }

    @GetMapping
    @Operation(summary = "Lista classificações salvas de forma paginada",
            description = "Retorna uma página de classificações persistidas, com filtros opcionais combináveis "
                    + "por título (parcial), categoria, status e palavra-chave.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de classificações"),
            @ApiResponse(responseCode = "400", description = "Parâmetro de paginação, ordenação ou filtro inválido")
    })
    public ResponseEntity<PaginaArtigosResponse> listar(
            @Parameter(description = "Número da página, começando em 0", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamanho da página (máximo 100)", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Campo e direção de ordenação, no formato campo,asc|desc. "
                    + "Campos aceitos: criadoEm, titulo, categoria, status, confianca", example = "criadoEm,desc")
            @RequestParam(defaultValue = "criadoEm,desc") String sort,
            @Parameter(description = "Filtro parcial por título, ignora maiúsculas/minúsculas", example = "spring")
            @RequestParam(required = false) String titulo,
            @Parameter(description = "Filtro exato por categoria (ignora maiúsculas/minúsculas)", example = "Backend")
            @RequestParam(required = false) String categoria,
            @Parameter(description = "Filtro por status persistido: APROVADO ou PENDENTE", example = "APROVADO")
            @RequestParam(required = false) String status,
            @Parameter(description = "Filtro por palavra-chave extraída do artigo", example = "java")
            @RequestParam(required = false) String palavraChave) {
        return ResponseEntity.ok(artigoService.listar(page, size, sort, titulo, categoria, status, palavraChave));
    }

    @GetMapping("/{id:\\d+}")
    @Operation(summary = "Busca uma classificação salva")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Classificação encontrada"),
            @ApiResponse(responseCode = "404", description = "Classificação não encontrada")
    })
    public ResponseEntity<ArtigoResponse> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(artigoService.buscar(id));
    }

    @PutMapping("/{id:\\d+}")
    @Operation(summary = "Edita título, texto, autores, link e ano de um conteúdo",
            description = "Não reclassifica o conteúdo — categoria e status seguem as regras da IA/moderação.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Conteúdo atualizado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Conteúdo não encontrado")
    })
    public ResponseEntity<ArtigoResponse> atualizar(@PathVariable Long id,
                                                     @Valid @RequestBody AtualizacaoArtigoRequest request) {
        return ResponseEntity.ok(artigoService.atualizar(id, request));
    }

    @DeleteMapping("/{id:\\d+}")
    @Operation(summary = "Exclui um conteúdo classificado")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Conteúdo excluído"),
            @ApiResponse(responseCode = "404", description = "Conteúdo não encontrado")
    })
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        artigoService.excluir(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id:\\d+}/moderacao")
    @Operation(summary = "Aplica decisão humana de moderação",
            description = "Somente para conteúdos em PENDENTE_MODERACAO. Aceita correção opcional de categoria. "
                    + "Não chama novamente a IA.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Moderação aplicada"),
            @ApiResponse(responseCode = "400", description = "Decisão inválida ou transição de status inválida"),
            @ApiResponse(responseCode = "404", description = "Conteúdo não encontrado")
    })
    public ResponseEntity<ArtigoResponse> moderar(@PathVariable Long id,
                                                   @Valid @RequestBody ModeracaoRequest request) {
        return ResponseEntity.ok(artigoService.moderar(id, request));
    }

    @GetMapping("/{id:\\d+}/feedback")
    @Operation(summary = "Lista o histórico de decisões humanas de moderação de um conteúdo")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Histórico de feedback"),
            @ApiResponse(responseCode = "404", description = "Conteúdo não encontrado")
    })
    public ResponseEntity<List<FeedbackResponse>> feedback(@PathVariable Long id) {
        return ResponseEntity.ok(artigoService.buscarFeedback(id));
    }

    @GetMapping("/estatisticas")
    @Operation(summary = "Estatísticas globais dos conteúdos classificados",
            description = "Calculadas no banco sobre todos os registros, não apenas a página carregada.")
    @ApiResponse(responseCode = "200", description = "Estatísticas globais")
    public ResponseEntity<EstatisticasResponse> estatisticas() {
        return ResponseEntity.ok(artigoService.estatisticas());
    }

    @GetMapping("/health")
    @Operation(summary = "Verifica a saúde da API")
    @ApiResponse(responseCode = "200", description = "API disponível")
    public Map<String, String> health() { return Map.of("status", "UP", "servico", "techmind-classificador"); }
}
