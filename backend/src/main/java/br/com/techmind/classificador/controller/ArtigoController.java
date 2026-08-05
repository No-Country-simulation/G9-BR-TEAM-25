package br.com.techmind.classificador.controller;

import br.com.techmind.classificador.dto.ClassificacaoRequest;
import br.com.techmind.classificador.dto.ClassificacaoResponse;
import br.com.techmind.classificador.service.ArtigoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;

import java.util.Map;
import java.util.List;
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
    @Operation(summary = "Lista classificações salvas")
    @ApiResponse(responseCode = "200", description = "Classificações persistidas")
    public ResponseEntity<List<ArtigoResponse>> listar() {
        return ResponseEntity.ok(artigoService.listar());
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
