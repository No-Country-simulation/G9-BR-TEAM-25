/**
 * TechMind - Integração do frontend com a API de artigos.
 *
 * @author Diego Pitoco
 */
const API_URL = import.meta.env.VITE_API_URL || ''

async function extrairMensagemDeErro(response) {
  try {
    const corpo = await response.json()
    return corpo?.mensagem || corpo?.erro || `Erro ${response.status} ao comunicar com a API.`
  } catch {
    return `Erro ${response.status} ao comunicar com a API.`
  }
}

async function request(path, options) {
  let response
  try {
    response = await fetch(`${API_URL}${path}`, options)
  } catch {
    throw new Error('Não foi possível conectar à API. Verifique se o backend está em execução.')
  }

  if (!response.ok) {
    throw new Error(await extrairMensagemDeErro(response))
  }

  return response.status === 204 ? null : response.json()
}

function construirQuery(parametros) {
  const query = new URLSearchParams()
  Object.entries(parametros).forEach(([chave, valor]) => {
    if (valor !== undefined && valor !== null && String(valor).trim() !== '') {
      query.set(chave, valor)
    }
  })
  const texto = query.toString()
  return texto ? `?${texto}` : ''
}

export async function checkHealth() {
  try {
    const response = await fetch(`${API_URL}/api/artigos/health`)
    return response.ok
  } catch {
    return false
  }
}

export function listarArtigos({ page = 0, size = 10, sort, titulo, categoria, status, palavraChave } = {}) {
  const query = construirQuery({ page, size, sort, titulo, categoria, status, palavraChave })
  return request(`/api/artigos${query}`)
}

export function buscarArtigo(id) {
  return request(`/api/artigos/${id}`)
}

export function classificarArtigo(payload) {
  return request('/api/artigos/classificar', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  })
}

export function atualizarArtigo(id, payload) {
  return request(`/api/artigos/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  })
}

export function excluirArtigo(id) {
  return request(`/api/artigos/${id}`, { method: 'DELETE' })
}

export function moderarArtigo(id, { decisao, categoriaCorrigida }) {
  return request(`/api/artigos/${id}/moderacao`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ decisao, categoriaCorrigida: categoriaCorrigida || null }),
  })
}

export function buscarFeedback(id) {
  return request(`/api/artigos/${id}/feedback`)
}

export function buscarEstatisticas() {
  return request('/api/artigos/estatisticas')
}
