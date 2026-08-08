/**
 * TechMind - Interface principal da central de conhecimento.
 *
 * @author Diego Pitoco
 */
import { useEffect, useState } from 'react'
import './App.css'
import {
  atualizarArtigo,
  buscarArtigo,
  buscarEstatisticas,
  buscarFeedback,
  checkHealth,
  classificarArtigo,
  excluirArtigo,
  listarArtigos,
  moderarArtigo,
} from './api/artigosApi'

const initialForm = { titulo: '', texto: '', autores: '', link: '', ano: '' }
const initialFiltros = { titulo: '', categoria: '', status: '', palavraChave: '' }
const initialFormEdicao = { titulo: '', texto: '', autores: '', link: '', ano: '' }
const TAMANHO_PAGINA = 10

function App() {
  const [form, setForm] = useState(initialForm)
  const [result, setResult] = useState(null)
  const [carregandoClassificacao, setCarregandoClassificacao] = useState(false)
  const [erroClassificacao, setErroClassificacao] = useState('')
  const [apiOnline, setApiOnline] = useState(false)

  const [estatisticas, setEstatisticas] = useState(null)

  const [artigos, setArtigos] = useState([])
  const [paginaAtual, setPaginaAtual] = useState(0)
  const [tamanhoPagina] = useState(TAMANHO_PAGINA)
  const [totalElementos, setTotalElementos] = useState(0)
  const [totalPaginas, setTotalPaginas] = useState(0)
  const [primeiraPagina, setPrimeiraPagina] = useState(true)
  const [ultimaPagina, setUltimaPagina] = useState(true)
  const [carregandoListagem, setCarregandoListagem] = useState(false)
  const [erroListagem, setErroListagem] = useState('')

  const [filtros, setFiltros] = useState(initialFiltros)

  const [artigoSelecionado, setArtigoSelecionado] = useState(null)
  const [carregandoDetalhes, setCarregandoDetalhes] = useState(false)
  const [erroDetalhes, setErroDetalhes] = useState('')

  const [edicaoAtiva, setEdicaoAtiva] = useState(false)
  const [formEdicao, setFormEdicao] = useState(initialFormEdicao)
  const [carregandoEdicao, setCarregandoEdicao] = useState(false)
  const [erroEdicao, setErroEdicao] = useState('')

  const [categoriaCorrigidaInput, setCategoriaCorrigidaInput] = useState('')
  const [carregandoModeracao, setCarregandoModeracao] = useState(false)
  const [erroModeracao, setErroModeracao] = useState('')

  const [carregandoExclusao, setCarregandoExclusao] = useState(false)
  const [erroExclusao, setErroExclusao] = useState('')

  const [feedbackHistorico, setFeedbackHistorico] = useState([])

  function categoryLabel(category) {
    return category && category.toLowerCase() !== 'indefinido' ? category : 'Sem categoria'
  }

  function statusLabel(status) {
    if (status === 'PENDENTE_MODERACAO') return 'Pendente de moderação'
    if (status === 'APROVADO') return 'Aprovado'
    if (status === 'REJEITADO') return 'Rejeitado'
    return status || 'CLASSIFICADO'
  }

  async function carregarEstatisticas() {
    try {
      const dados = await buscarEstatisticas()
      setEstatisticas(dados)
    } catch {
      // Estatísticas são complementares: uma falha aqui não deve travar o restante da tela.
    }
  }

  async function carregarArtigos(pagina, filtrosParaConsulta) {
    setCarregandoListagem(true)
    setErroListagem('')
    try {
      const resposta = await listarArtigos({
        page: pagina,
        size: tamanhoPagina,
        titulo: filtrosParaConsulta.titulo,
        categoria: filtrosParaConsulta.categoria,
        status: filtrosParaConsulta.status,
        palavraChave: filtrosParaConsulta.palavraChave,
      })
      setArtigos(resposta.conteudo)
      setArtigoSelecionado((atual) => (atual && resposta.conteudo.some((item) => item.id === atual.id) ? atual : null))
      setPaginaAtual(resposta.pagina)
      setTotalElementos(resposta.totalElementos)
      setTotalPaginas(resposta.totalPaginas)
      setPrimeiraPagina(resposta.primeira)
      setUltimaPagina(resposta.ultima)
      setApiOnline(true)
    } catch (requestError) {
      setErroListagem(requestError.message || 'Não foi possível carregar os conteúdos.')
      setArtigos([])
    } finally {
      setCarregandoListagem(false)
    }
  }

  useEffect(() => {
    checkHealth().then(setApiOnline)
    carregarArtigos(0, initialFiltros)
    carregarEstatisticas()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  function updateField(event) {
    setForm((current) => ({ ...current, [event.target.name]: event.target.value }))
  }

  function atualizarFiltro(event) {
    const { name, value } = event.target
    setFiltros((current) => ({ ...current, [name]: value }))
  }

  function aplicarFiltros(event) {
    event.preventDefault()
    carregarArtigos(0, filtros)
  }

  function limparFiltros() {
    setFiltros(initialFiltros)
    carregarArtigos(0, initialFiltros)
  }

  function irParaPaginaAnterior() {
    if (!primeiraPagina && paginaAtual > 0) {
      carregarArtigos(paginaAtual - 1, filtros)
    }
  }

  function irParaProximaPagina() {
    if (!ultimaPagina) {
      carregarArtigos(paginaAtual + 1, filtros)
    }
  }

  async function selecionarArtigo(id) {
    setCarregandoDetalhes(true)
    setErroDetalhes('')
    setArtigoSelecionado(null)
    setEdicaoAtiva(false)
    setErroEdicao('')
    setErroModeracao('')
    setErroExclusao('')
    setCategoriaCorrigidaInput('')
    setFeedbackHistorico([])
    try {
      const detalhe = await buscarArtigo(id)
      setArtigoSelecionado(detalhe)
      buscarFeedback(id).then(setFeedbackHistorico).catch(() => setFeedbackHistorico([]))
    } catch (requestError) {
      setErroDetalhes(requestError.message || 'Não foi possível carregar os detalhes.')
    } finally {
      setCarregandoDetalhes(false)
    }
  }

  function fecharDetalhes() {
    setArtigoSelecionado(null)
    setErroDetalhes('')
    setEdicaoAtiva(false)
  }

  async function classify(event) {
    event.preventDefault()
    if (carregandoClassificacao) {
      return
    }
    setCarregandoClassificacao(true)
    setErroClassificacao('')

    const payload = { ...form, ano: form.ano ? Number(form.ano) : null }

    try {
      const data = await classificarArtigo(payload)
      setApiOnline(true)
      setResult(data)
      await carregarArtigos(paginaAtual, filtros)
      await carregarEstatisticas()
    } catch (requestError) {
      setErroClassificacao(requestError.message || 'Não foi possível classificar o conteúdo no momento. Tente novamente.')
    } finally {
      setCarregandoClassificacao(false)
    }
  }

  function iniciarEdicao() {
    setFormEdicao({
      titulo: artigoSelecionado.titulo || '',
      texto: artigoSelecionado.texto || '',
      autores: artigoSelecionado.autores || '',
      link: artigoSelecionado.link || '',
      ano: artigoSelecionado.ano || '',
    })
    setErroEdicao('')
    setEdicaoAtiva(true)
  }

  function cancelarEdicao() {
    setEdicaoAtiva(false)
    setErroEdicao('')
  }

  function atualizarCampoEdicao(event) {
    setFormEdicao((current) => ({ ...current, [event.target.name]: event.target.value }))
  }

  async function salvarEdicao(event) {
    event.preventDefault()
    if (carregandoEdicao) return
    setCarregandoEdicao(true)
    setErroEdicao('')
    try {
      const payload = { ...formEdicao, ano: formEdicao.ano ? Number(formEdicao.ano) : null }
      const atualizado = await atualizarArtigo(artigoSelecionado.id, payload)
      setArtigoSelecionado(atualizado)
      setEdicaoAtiva(false)
      await carregarArtigos(paginaAtual, filtros)
    } catch (requestError) {
      setErroEdicao(requestError.message || 'Não foi possível salvar as alterações. Tente novamente.')
    } finally {
      setCarregandoEdicao(false)
    }
  }

  async function aplicarModeracao(decisao) {
    if (carregandoModeracao) return
    setCarregandoModeracao(true)
    setErroModeracao('')
    try {
      const atualizado = await moderarArtigo(artigoSelecionado.id, {
        decisao,
        categoriaCorrigida: categoriaCorrigidaInput,
      })
      setArtigoSelecionado(atualizado)
      setCategoriaCorrigidaInput('')
      buscarFeedback(atualizado.id).then(setFeedbackHistorico).catch(() => {})
      await carregarArtigos(paginaAtual, filtros)
      await carregarEstatisticas()
    } catch (requestError) {
      setErroModeracao(requestError.message || 'Não foi possível aplicar a moderação. Tente novamente.')
    } finally {
      setCarregandoModeracao(false)
    }
  }

  async function confirmarExclusao() {
    if (carregandoExclusao || !artigoSelecionado) return
    const confirmado = window.confirm('Tem certeza de que deseja excluir este conteúdo?')
    if (!confirmado) return
    setCarregandoExclusao(true)
    setErroExclusao('')
    try {
      await excluirArtigo(artigoSelecionado.id)
      setArtigoSelecionado(null)
      await carregarArtigos(0, filtros)
      await carregarEstatisticas()
    } catch (requestError) {
      setErroExclusao(requestError.message || 'Não foi possível excluir o conteúdo. Tente novamente.')
    } finally {
      setCarregandoExclusao(false)
    }
  }

  return (
    <main className="app-shell">
      <header className="topbar">
        <a className="brand" href="/" aria-label="TechMind início">
          <span className="brand-mark">T</span>
          <span>TechMind <small>Evolution</small><b>&lt;/&gt; central de conhecimento</b></span>
        </a>
        <div className="top-actions"><span className={`api-status ${apiOnline ? '' : 'offline'}`} title="Verifica apenas a disponibilidade do backend Java"><i /> API {apiOnline ? 'online' : 'offline'}</span><a href={import.meta.env.VITE_SWAGGER_URL || 'http://localhost:8080/swagger-ui.html'} target="_blank" rel="noreferrer">? Swagger</a><span className="user">Equipe<br /><small>TechMind</small></span></div>
      </header>

      <nav className="content-nav" aria-label="Navegação principal"><span className="nav-indicator" aria-current="page">▱ Meus conteúdos</span></nav>

      <section className="hero">
        <div><p className="eyebrow">CENTRAL DE CONHECIMENTO</p><h1>Conteúdos <em>inteligentes</em></h1><p className="subtitle">Classifique conteúdos técnicos, descubra palavras-chave e encontre materiais relacionados.</p></div>
        <button className="new-content" onClick={() => document.getElementById('titulo')?.focus()}>＋ Novo conteúdo</button>
      </section>

      <section className="metrics">
        <div><span>TOTAL DE CONTEÚDOS</span><strong>{estatisticas ? estatisticas.total : '—'}</strong><small>Global (Oracle)</small></div>
        <div><span>APROVADOS</span><strong>{estatisticas ? estatisticas.aprovados : '—'}</strong><small>Status atual</small></div>
        <div><span>PENDENTES</span><strong>{estatisticas ? estatisticas.pendentesModeracao : '—'}</strong><small>Aguardando moderação</small></div>
        <div><span>REJEITADOS</span><strong>{estatisticas ? estatisticas.rejeitados : '—'}</strong><small>Status atual</small></div>
        <div><span>CATEGORIAS</span><strong>{estatisticas ? estatisticas.quantidadeCategorias : '—'}</strong><small>Distintas</small></div>
        <div className="highlight"><span>CONFIANÇA MÉDIA</span><strong>{estatisticas ? `${Math.round(estatisticas.confiancaMedia * 100)}%` : '—'}</strong><small>Global</small></div>
      </section>

      {estatisticas?.distribuicaoPorCategoria && Object.keys(estatisticas.distribuicaoPorCategoria).length > 0 && (
        <div className="category-distribution">
          {Object.entries(estatisticas.distribuicaoPorCategoria).map(([categoria, quantidade]) => (
            <span key={categoria}>{categoryLabel(categoria)} <b>{quantidade}</b></span>
          ))}
        </div>
      )}

      <section className="workspace">
        <form className="card form-card" onSubmit={classify}>
          <div className="card-heading">
            <div><span className="step">01</span><h2>Conte sobre o artigo</h2></div>
            <span className="required">* Campos obrigatórios</span>
          </div>

          <label htmlFor="titulo">Título <span>*</span></label>
          <input id="titulo" name="titulo" value={form.titulo} onChange={updateField} placeholder="Ex.: Construindo APIs REST com Spring Boot" required minLength="3" maxLength="200" />

          <label htmlFor="texto">Texto do artigo <span>*</span></label>
          <textarea id="texto" name="texto" value={form.texto} onChange={updateField} placeholder="Cole aqui o conteúdo ou um resumo do artigo..." required minLength="10" rows="7" />
          <div className="field-hint">Mínimo de 10 caracteres</div>

          <div className="form-grid">
            <div><label htmlFor="autores">Autores</label><input id="autores" name="autores" value={form.autores} onChange={updateField} placeholder="Ex.: Equipe TechMind" /></div>
            <div><label htmlFor="ano">Ano</label><input id="ano" name="ano" value={form.ano} onChange={updateField} type="number" min="1900" max="2100" placeholder="2026" /></div>
          </div>
          <label htmlFor="link">Link do artigo</label>
          <input id="link" name="link" value={form.link} onChange={updateField} type="url" placeholder="https://exemplo.com/artigo" />

          {erroClassificacao && <p className="error-message" role="alert">{erroClassificacao}</p>}
          <button className="primary-button" type="submit" disabled={carregandoClassificacao}>
            {carregandoClassificacao ? <><span className="spinner" aria-hidden="true" /> Analisando conteúdo...</> : <>Classificar artigo <span>→</span></>}
          </button>
        </form>

        <aside className="card result-card" aria-live="polite">
          <div className="card-heading"><div><span className="step">02</span><h2>Resultado</h2></div></div>
          {result ? <div className="result-content">
            <div className="category-label">CATEGORIA IDENTIFICADA</div>
            <div className="category">{categoryLabel(result.categoria)}</div>
            <div className="confidence-row"><span>Confiança</span><strong>{Math.round(result.confianca * 100)}%</strong></div>
            <div className="progress"><span style={{ width: `${result.confianca * 100}%` }} /></div>
            <div className={`status ${result.status?.toLowerCase()}`}>{statusLabel(result.status)}</div>
            <div className="keywords"><h3>Palavras-chave</h3><div>{(result.palavrasChave || []).map((keyword) => <span key={keyword}>{keyword}</span>)}</div></div>
            <div className="related">
              <h3>Conteúdos relacionados</h3>
              <p className="related-hint">Recomendações geradas neste processamento — não ficam salvas no histórico.</p>
              {result.artigosRelacionados?.length ? <div className="related-list">{result.artigosRelacionados.map((relacionado) => (
                <div className="related-item" key={relacionado.id}>
                  <div><strong>{relacionado.titulo}</strong><small>{categoryLabel(relacionado.categoria)}</small></div>
                  <b>{Math.round((relacionado.scoreSimilaridade || 0) * 100)}%</b>
                </div>
              ))}</div> : <p className="no-history">Nenhum conteúdo relacionado desta vez.</p>}
            </div>
          </div> : <div className="empty-result"><div className="spark">✦</div><p>Seu resultado aparecerá aqui</p><span>Preencha os dados ao lado e clique em classificar.</span></div>}
        </aside>
      </section>

      <section className="card filters-card">
        <form onSubmit={aplicarFiltros}>
          <div className="card-heading"><div><h2>Filtros</h2></div></div>
          <div className="filters-grid">
            <div><label htmlFor="filtroTitulo">Título</label><input id="filtroTitulo" name="titulo" value={filtros.titulo} onChange={atualizarFiltro} placeholder="Buscar por título" /></div>
            <div><label htmlFor="filtroCategoria">Categoria</label><input id="filtroCategoria" name="categoria" value={filtros.categoria} onChange={atualizarFiltro} placeholder="Ex.: Backend" /></div>
            <div>
              <label htmlFor="filtroStatus">Status</label>
              <select id="filtroStatus" name="status" value={filtros.status} onChange={atualizarFiltro}>
                <option value="">Todos</option>
                <option value="APROVADO">Aprovado</option>
                <option value="PENDENTE_MODERACAO">Pendente de moderação</option>
                <option value="REJEITADO">Rejeitado</option>
              </select>
            </div>
            <div><label htmlFor="filtroPalavraChave">Palavra-chave</label><input id="filtroPalavraChave" name="palavraChave" value={filtros.palavraChave} onChange={atualizarFiltro} placeholder="Ex.: java" /></div>
          </div>
          <div className="filters-actions">
            <button type="submit" className="secondary-button" disabled={carregandoListagem}>Filtrar</button>
            <button type="button" className="ghost-button" onClick={limparFiltros} disabled={carregandoListagem}>Limpar filtros</button>
          </div>
        </form>
      </section>

      <section className="history-section">
        <div className="section-heading"><div><p className="eyebrow">ACERVO RECENTE</p><h2>Conteúdos classificados</h2></div><span>{totalElementos} registros</span></div>
        {erroListagem && <p className="error-message" role="alert">{erroListagem}</p>}
        {carregandoListagem ? <p className="no-history">Carregando conteúdos...</p> : (
          artigos.length ? <div className="history-list">{artigos.map((item) => (
            <button type="button" className="history-item" key={item.id} onClick={() => selecionarArtigo(item.id)}>
              <span className="history-icon">↗</span>
              <div><strong>{item.titulo || 'Artigo sem título'}</strong><small>{categoryLabel(item.categoria)}</small></div>
              {item.status && <span className={`status ${item.status.toLowerCase()}`}>{statusLabel(item.status)}</span>}
              <b>{item.confianca ? `${Math.round(item.confianca * 100)}%` : '—'}</b>
            </button>
          ))}</div> : <p className="no-history">Nenhuma classificação encontrada com os filtros atuais.</p>
        )}
        <div className="pagination">
          <button type="button" onClick={irParaPaginaAnterior} disabled={primeiraPagina || carregandoListagem}>← Anterior</button>
          <span>Página {totalPaginas ? paginaAtual + 1 : 0} de {totalPaginas}</span>
          <button type="button" onClick={irParaProximaPagina} disabled={ultimaPagina || carregandoListagem}>Próxima →</button>
        </div>
      </section>

      {(artigoSelecionado || carregandoDetalhes || erroDetalhes) && <section className="card details-card">
        <div className="card-heading">
          <div><span className="step">03</span><h2>Detalhes do conteúdo</h2></div>
          <button type="button" className="ghost-button" onClick={fecharDetalhes}>Fechar</button>
        </div>
        {carregandoDetalhes && <p className="no-history">Carregando detalhes...</p>}
        {erroDetalhes && <p className="error-message" role="alert">{erroDetalhes}</p>}
        {artigoSelecionado && !edicaoAtiva && <div className="details-content">
          <div className="category-label">CATEGORIA</div>
          <div className="category">{categoryLabel(artigoSelecionado.categoria)}</div>
          {artigoSelecionado.categoriaOriginal && artigoSelecionado.categoriaOriginal !== artigoSelecionado.categoria && (
            <p className="category-original">Categoria original identificada pela IA: {categoryLabel(artigoSelecionado.categoriaOriginal)}</p>
          )}
          <h3>{artigoSelecionado.titulo}</h3>
          <p className="details-text">{artigoSelecionado.texto}</p>
          <div className="details-grid">
            <div><span>Autores</span><strong>{artigoSelecionado.autores || '—'}</strong></div>
            <div><span>Ano</span><strong>{artigoSelecionado.ano || '—'}</strong></div>
            <div><span>Confiança</span><strong>{Math.round(artigoSelecionado.confianca * 100)}%</strong></div>
            <div><span>Status</span><strong>{statusLabel(artigoSelecionado.status)}</strong></div>
            <div><span>Criado em</span><strong>{artigoSelecionado.criadoEm ? new Date(artigoSelecionado.criadoEm).toLocaleString('pt-BR') : '—'}</strong></div>
          </div>
          {artigoSelecionado.link && <p><a href={artigoSelecionado.link} target="_blank" rel="noreferrer">Abrir link original ↗</a></p>}
          <div className="keywords"><h3>Palavras-chave</h3><div>{(artigoSelecionado.palavrasChave || []).map((keyword) => <span key={keyword}>{keyword}</span>)}</div></div>

          {artigoSelecionado.status === 'PENDENTE_MODERACAO' && (
            <div className="moderation-panel">
              <h3>Moderação</h3>
              <label htmlFor="categoriaCorrigida">Corrigir categoria (opcional)</label>
              <input id="categoriaCorrigida" value={categoriaCorrigidaInput} onChange={(event) => setCategoriaCorrigidaInput(event.target.value)} placeholder={artigoSelecionado.categoria} />
              {erroModeracao && <p className="error-message" role="alert">{erroModeracao}</p>}
              <div className="moderation-actions">
                <button type="button" className="secondary-button" onClick={() => aplicarModeracao('APROVADO')} disabled={carregandoModeracao}>{carregandoModeracao ? 'Aplicando...' : '✓ Aprovar'}</button>
                <button type="button" className="danger-button" onClick={() => aplicarModeracao('REJEITADO')} disabled={carregandoModeracao}>{carregandoModeracao ? 'Aplicando...' : '✕ Rejeitar'}</button>
              </div>
            </div>
          )}

          {feedbackHistorico.length > 0 && (
            <div className="feedback-history">
              <h3>Histórico de moderação</h3>
              {feedbackHistorico.map((item) => (
                <div className="feedback-item" key={item.id}>
                  <strong>{statusLabel(item.decisao)}</strong> em {new Date(item.decididoEm).toLocaleString('pt-BR')}
                  {item.categoriaCorrigida ? ` — categoria corrigida para ${categoryLabel(item.categoriaCorrigida)}` : ''}
                </div>
              ))}
            </div>
          )}

          {erroExclusao && <p className="error-message" role="alert">{erroExclusao}</p>}
          <div className="details-actions">
            <button type="button" className="secondary-button" onClick={iniciarEdicao}>✎ Editar conteúdo</button>
            <button type="button" className="danger-button" onClick={confirmarExclusao} disabled={carregandoExclusao}>{carregandoExclusao ? 'Excluindo...' : '🗑 Excluir'}</button>
          </div>
        </div>}

        {artigoSelecionado && edicaoAtiva && (
          <form className="edit-form" onSubmit={salvarEdicao}>
            <label htmlFor="editTitulo">Título</label>
            <input id="editTitulo" name="titulo" value={formEdicao.titulo} onChange={atualizarCampoEdicao} required minLength="3" maxLength="200" />
            <label htmlFor="editTexto">Texto</label>
            <textarea id="editTexto" name="texto" value={formEdicao.texto} onChange={atualizarCampoEdicao} required minLength="10" rows="6" />
            <div className="form-grid">
              <div><label htmlFor="editAutores">Autores</label><input id="editAutores" name="autores" value={formEdicao.autores} onChange={atualizarCampoEdicao} /></div>
              <div><label htmlFor="editAno">Ano</label><input id="editAno" name="ano" type="number" min="1900" max="2100" value={formEdicao.ano} onChange={atualizarCampoEdicao} /></div>
            </div>
            <label htmlFor="editLink">Link</label>
            <input id="editLink" name="link" type="url" value={formEdicao.link} onChange={atualizarCampoEdicao} />
            {erroEdicao && <p className="error-message" role="alert">{erroEdicao}</p>}
            <div className="details-actions">
              <button type="submit" className="secondary-button" disabled={carregandoEdicao}>{carregandoEdicao ? 'Salvando...' : 'Salvar alterações'}</button>
              <button type="button" className="ghost-button" onClick={cancelarEdicao} disabled={carregandoEdicao}>Cancelar</button>
            </div>
          </form>
        )}
      </section>}
    </main>
  )
}

export default App
