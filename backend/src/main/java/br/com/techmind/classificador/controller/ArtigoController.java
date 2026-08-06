package br.com.techmind.classificador.controller;

import br.com.techmind.classificador.dto.ClassificacaoRequest;
import br.com.techmind.classificador.dto.ClassificacaoResponse;
import br.com.techmind.classificador.dto.PaginaArtigosResponse;
import br.com.techmind.classificador.service.ArtigoService;
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

    @GetMapping("/{id}")
    @Operation(summary = "Busca uma classificação salva")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Classificação encontrada"),
            @ApiResponse(responseCode = "404", description = "Classificação não encontrada")
    })
    public ResponseEntity<ArtigoResponse> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(artigoService.buscar(id));
    }

    @GetMapping("/health")
    @Operation(summary = "Verifica a saúde da API")
    @ApiResponse(responseCode = "200", description = "API disponível")
    public Map<String, String> health() { return Map.of("status", "UP", "servico", "techmind-classificador"); }
}
