package com.portabilidade.api.operacao.hash;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;

import com.portabilidade.api.operacao.dto.SolicitarPortabilidadeRequest;
import org.junit.jupiter.api.Test;

class RequestHashServiceTests {

	private final RequestHashService requestHashService = new RequestHashService();

	@Test
	void deveGerarHashDeterministicoParaPayloadNormalizado() {
		SolicitarPortabilidadeRequest primeiro = new SolicitarPortabilidadeRequest(
				" BEN001 ", List.of("P002", "P001"), " conta-123 ");
		SolicitarPortabilidadeRequest equivalente = new SolicitarPortabilidadeRequest(
				"BEN001", List.of("P001", "P002"), "conta-123");

		assertEquals(requestHashService.gerar(primeiro), requestHashService.gerar(equivalente));
	}

	@Test
	void deveGerarHashDiferenteParaPayloadDiferente() {
		SolicitarPortabilidadeRequest primeiro = new SolicitarPortabilidadeRequest(
				"BEN001", List.of("P001", "P002"), "conta-123");
		SolicitarPortabilidadeRequest diferente = new SolicitarPortabilidadeRequest(
				"BEN001", List.of("P001"), "conta-123");

		assertNotEquals(requestHashService.gerar(primeiro), requestHashService.gerar(diferente));
	}
}
