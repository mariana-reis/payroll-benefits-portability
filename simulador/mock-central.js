// mock-central.js
// Simulador provisório da Central de Convênios

const http = require("http");
const { randomUUID } = require("crypto");

const AGENDAS = {
  BEN001: [
    {
      parcelaId: "P001",
      convenioId: "CONV01",
      valor: 150.0,
      dataDesconto: "2026-01-10",
      status: "ABERTA",
    },
    {
      parcelaId: "P002",
      convenioId: "CONV01",
      valor: 150.0,
      dataDesconto: "2026-02-10",
      status: "ABERTA",
    },
  ],

  BEN002: [
    {
      parcelaId: "P010",
      convenioId: "CONV02",
      valor: 300.0,
      dataDesconto: "2026-01-15",
      status: "ABERTA",
    },
  ],
};

const operacoes = new Map();

function send(res, status, body, extraHeaders = {}) {
  const payload =
    body === undefined ? "" : JSON.stringify(body);

  res.writeHead(status, {
    "Content-Type": "application/json",
    ...extraHeaders,
  });

  res.end(payload);
}

function readBody(req) {
  return new Promise((resolve) => {
    let data = "";

    req.on("data", (chunk) => {
      data += chunk;
    });

    req.on("end", () => {
      try {
        resolve(data ? JSON.parse(data) : {});
      } catch {
        resolve({});
      }
    });
  });
}

const server = http.createServer(async (req, res) => {
  const url = new URL(req.url, "http://localhost");

  const parts = url.pathname
    .split("/")
    .filter(Boolean);

  /**
   * POST /autorizacoes
   */
  if (
    req.method === "POST" &&
    parts[0] === "autorizacoes"
  ) {
    await readBody(req);

    return send(res, 201, {
      autorizacaoId: randomUUID(),
    });
  }

  /**
   * GET /beneficiarios/{id}/agenda
   */
  if (
    req.method === "GET" &&
    parts[0] === "beneficiarios" &&
    parts[2] === "agenda"
  ) {
    const beneficiarioId = parts[1];

    return send(
      res,
      200,
      AGENDAS[beneficiarioId] || []
    );
  }

  /**
   * POST /operacoes-portabilidade
   */
  if (
    req.method === "POST" &&
    parts[0] === "operacoes-portabilidade"
  ) {
    const body = await readBody(req);

    if (body.beneficiarioId === "BEN_ERRO") {
      return send(res, 503, {
        erro:
          "Central indisponível no momento (simulado). Tente novamente.",
      });
    }

    const protocolo = randomUUID();

    const vaiRejeitar =
      body.beneficiarioId === "BEN002";

    operacoes.set(protocolo, {
      resolveEm: Date.now() + 5000,
      vaiRejeitar,
    });

    return send(res, 202, {
      protocolo,
    });
  }

  /**
   * GET /operacoes-portabilidade/{protocolo}
   */
  if (
    req.method === "GET" &&
    parts[0] === "operacoes-portabilidade" &&
    parts[1]
  ) {
    const protocolo = parts[1];

    const operacao =
      operacoes.get(protocolo);

    if (!operacao) {
      return send(res, 404, {
        erro: "Protocolo não encontrado",
      });
    }

    if (Date.now() < operacao.resolveEm) {
      return send(
        res,
        204,
        undefined,
        {
          "Retry-After": "2",
        }
      );
    }

    return send(res, 200, {
      status: operacao.vaiRejeitar
        ? "REJEITADA"
        : "CONFIRMADA",

      motivoRejeicao: operacao.vaiRejeitar
        ? "Beneficiário atingiu o limite de margem consignável"
        : null,
    });
  }

  return send(res, 404, {
    erro: "Rota não encontrada",
  });
});

server.listen(4000, () => {
  console.log(
    "Central de Convênios simulada em http://localhost:4000"
  );
});