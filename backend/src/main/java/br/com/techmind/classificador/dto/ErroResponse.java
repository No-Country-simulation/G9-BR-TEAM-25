package br.com.techmind.classificador.dto;

import java.time.Instant;

public record ErroResponse(Instant timestamp, int status, String erro, String mensagem, String path) {
}
