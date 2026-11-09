package com.portabilidade.api.central.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class HmacSignerTests {

	@Test
	void deveGerarAssinaturaValida() {
		HmacSigner signer = new HmacSigner("test-secret");
		byte[] body = "{\"beneficiarioId\":\"BEN001\"}".getBytes(StandardCharsets.UTF_8);

		assertEquals(
				"e1d55a7e3639c4774300564df3a15dc3b8fcf8d67dddaeaba98f464fd8781c50",
				signer.sign("1690000000", body));
	}

	@Test
	void deveReproduzirVetorConhecido() {
		HmacSigner signer = new HmacSigner("Jefe");
		byte[] message = "what do ya want for nothing?".getBytes(StandardCharsets.US_ASCII);

		assertEquals(
				"5bdcc146bf60754e6a042426089575c75a003f089d2739839dec58b964ec3843",
				signer.signMessage(message));
	}

	@Test
	void deveGerarAssinaturasDiferentesParaTimestampOuPayloadDiferente() {
		HmacSigner signer = new HmacSigner("test-secret");
		byte[] body = "{\"valor\":1}".getBytes(StandardCharsets.UTF_8);

		assertFalse(HmacSigner.signaturesMatch(signer.sign("100", body), signer.sign("101", body)));
		assertFalse(HmacSigner.signaturesMatch(
				signer.sign("100", body),
				signer.sign("100", "{\"valor\":2}".getBytes(StandardCharsets.UTF_8))));
	}

	@Test
	void deveCompararAssinaturasEmTempoConstante() {
		assertTrue(HmacSigner.signaturesMatch("a1b2", "a1b2"));
		assertFalse(HmacSigner.signaturesMatch("a1b2", "a1b3"));
		assertFalse(HmacSigner.signaturesMatch("a1b2", null));
	}

	@Test
	void deveFalharComSegredoInvalido() {
		assertThrows(IllegalArgumentException.class, () -> new HmacSigner(""));
		assertThrows(IllegalArgumentException.class, () -> new HmacSigner(null));
	}
}
