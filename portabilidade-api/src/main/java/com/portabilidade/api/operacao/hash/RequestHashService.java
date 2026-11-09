package com.portabilidade.api.operacao.hash;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

import com.portabilidade.api.operacao.dto.SolicitarPortabilidadeRequest;
import org.springframework.stereotype.Service;

@Service
public class RequestHashService {

	public String gerar(SolicitarPortabilidadeRequest request) {
		Objects.requireNonNull(request, "A requisição é obrigatória.");
		if (request.beneficiarioId() == null || request.contaDestino() == null
				|| request.parcelaIds() == null) {
			throw new IllegalArgumentException("Os campos da requisição são obrigatórios.");
		}

		try {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			try (DataOutputStream output = new DataOutputStream(bytes)) {
				escrever(output, request.beneficiarioId().trim());
				escrever(output, request.contaDestino().trim());

				List<String> parcelaIds = request.parcelaIds().stream()
						.map(parcelaId -> Objects.requireNonNull(parcelaId).trim())
						.sorted()
						.toList();
				output.writeInt(parcelaIds.size());
				for (String parcelaId : parcelaIds) {
					escrever(output, parcelaId);
				}
			}

			byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray());
			return HexFormat.of().formatHex(digest);
		} catch (IOException exception) {
			throw new IllegalStateException("Não foi possível normalizar a requisição para idempotência.", exception);
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 não está disponível nesta JVM.", exception);
		}
	}

	private void escrever(DataOutputStream output, String valor) throws IOException {
		byte[] bytes = valor.getBytes(StandardCharsets.UTF_8);
		output.writeInt(bytes.length);
		output.write(bytes);
	}
}
