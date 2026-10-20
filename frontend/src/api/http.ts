/** Cliente HTTP mínimo. Em dev o Vite repassa /api ao back end (porta 8080); em produção, mesma origem. */
const BASE = (import.meta.env.VITE_API_URL as string | undefined) ?? ''

export class ApiError extends Error {
  constructor(message: string, readonly status: number, readonly detalhes: string[] = []) {
    super(message)
  }
}

async function chamar<T>(metodo: string, caminho: string, corpo?: unknown): Promise<T> {
  let resp: Response
  try {
    resp = await fetch(`${BASE}${caminho}`, {
      method: metodo,
      headers: corpo === undefined ? undefined : { 'Content-Type': 'application/json' },
      body: corpo === undefined ? undefined : JSON.stringify(corpo),
    })
  } catch {
    throw new ApiError('Sem conexão com o servidor. Verifique se o back end está rodando.', 0)
  }
  if (!resp.ok) {
    let mensagem = `Erro ${resp.status}`
    let detalhes: string[] = []
    try {
      const e = (await resp.json()) as { mensagem?: string; detalhes?: string[] }
      if (e.mensagem) mensagem = e.mensagem
      detalhes = e.detalhes ?? []
    } catch {
      /* resposta sem corpo JSON */
    }
    throw new ApiError(mensagem, resp.status, detalhes)
  }
  return resp.status === 204 ? (undefined as T) : ((await resp.json()) as T)
}

export const http = {
  get: <T>(caminho: string) => chamar<T>('GET', caminho),
  post: <T>(caminho: string, corpo?: unknown) => chamar<T>('POST', caminho, corpo ?? {}),
  put: <T>(caminho: string, corpo: unknown) => chamar<T>('PUT', caminho, corpo),
  delete: (caminho: string) => chamar<void>('DELETE', caminho),
}
