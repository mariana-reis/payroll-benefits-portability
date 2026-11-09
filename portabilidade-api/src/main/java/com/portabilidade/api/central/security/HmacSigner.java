package com.portabilidade.api.central.security;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class HmacSigner {

	private static final String ALGORITHM = "HmacSHA256";
	private static final byte SEPARATOR = (byte) '.';

	private final SecretKeySpec secretKey;

	public HmacSigner(String secret) {
		if (secret == null || secret.isBlank()) {
			throw new IllegalArgumentException("O segredo HMAC da Central é obrigatório.");
		}
		this.secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM);
	}

	public String sign(String timestamp, byte[] body) {
		Objects.requireNonNull(timestamp, "timestamp não pode ser nulo");
		Objects.requireNonNull(body, "body não pode ser nulo");

		Mac mac = criarMac();
		mac.update(timestamp.getBytes(StandardCharsets.UTF_8));
		mac.update(SEPARATOR);
		return HexFormat.of().formatHex(mac.doFinal(body));
	}

	String signMessage(byte[] message) {
		Objects.requireNonNull(message, "message não pode ser nula");
		return HexFormat.of().formatHex(criarMac().doFinal(message));
	}

	public static boolean signaturesMatch(String expected, String actual) {
		if (expected == null || actual == null) {
			return false;
		}
		return MessageDigest.isEqual(
				expected.getBytes(StandardCharsets.US_ASCII),
				actual.getBytes(StandardCharsets.US_ASCII));
	}

	private Mac criarMac() {
		try {
			Mac mac = Mac.getInstance(ALGORITHM);
			mac.init(secretKey);
			return mac;
		} catch (NoSuchAlgorithmException | InvalidKeyException exception) {
			throw new IllegalStateException("Não foi possível inicializar HMAC-SHA256.", exception);
		}
	}
}
