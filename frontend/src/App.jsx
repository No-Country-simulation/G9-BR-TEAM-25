import { useEffect, useState } from 'react'
import './App.css'
import { buscarArtigo, checkHealth, classificarArtigo, listarArtigos } from './api/artigosApi'

const initialForm = { titulo: '', texto: '', autores: '', link: '', ano: '' }
const initialFiltros = { titulo: '', categoria: '', status: '', palavraChave: '' }
const TAMANHO_PAGINA = 10

function App() {
  const [form, setForm] = useState(initialForm)
  const [result, setResult] = useState(null)
  const [carregandoClassificacao, setCarregandoClassificacao] = useState(false)
  const [erroClassificacao, setErroClassificacao] = useState('')
  const [apiOnline, setApiOnline] = useState(false)

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

  function categoryLabel(category) {
    return category && category.toLowerCase() !== 'indefinido' ? category : 'Sem categoria'
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
    try {
      const detalhe = await buscarArtigo(id)
      setArtigoSelecionado(detalhe)
    } catch (requestError) {
      setErroDetalhes(requestError.message || 'Não foi possível carregar os detalhes.')
    } finally {
      setCarregandoDetalhes(false)
    }
  }

  function fecharDetalhes() {
    setArtigoSelecionado(null)
    setErroDetalhes('')
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
    } catch (requestError) {
      setErroClassificacao(requestError.message || 'Verifique se o backend está em execução.')
    } finally {
      setCarregandoClassificacao(false)
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

      <nav className="content-nav" aria-label="Navegação principal"><button className="active">▱ Meus conteúdos</button><button>＋ Novo conteúdo</button><button className="disabled">♢ Moderação <small>em breve</small></button></nav>

      <section className="hero">
        <div><p className="eyebrow">CENTRAL DE CONHECIMENTO</p><h1>Conteúdos <em>inteligentes</em></h1><p className="subtitle">Classifique artigos técnicos e organize seu conhecimento com inteligência artificial.</p></div>
        <button className="new-content" onClick={() => document.getElementById('titulo')?.focus()}>＋ Novo conteúdo</button>
      </section>

      <section className="metrics"><div><span>CONTEÚDOS VISÍVEIS</span><strong>{totalElementos}</strong><small>Classificações salvas</small></div><div><span>CATEGORIAS</span><strong>{new Set(artigos.map((item) => categoryLabel(item.categoria)).filter((category) => category !== 'Sem categoria')).size}</strong><small>Nesta página</small></div><div className="highlight"><span>CONFIANÇA MÉDIA</span><strong>{artigos.length ? `${Math.round(artigos.reduce((sum, item) => sum + (item.confianca || 0), 0) / artigos.length * 100)}%` : '—'}</strong><small>Nesta página</small></div></section>

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
          <button className="primary-button" type="submit" disabled={carregandoClassificacao}>{carregandoClassificacao ? 'Classificando...' : 'Classificar artigo'} <span>→</span></button>
        </form>

        <aside className="card result-card">
          <div className="card-heading"><div><span className="step">02</span><h2>Resultado</h2></div></div>
          {result ? <div className="result-content">
            <div className="category-label">CATEGORIA IDENTIFICADA</div>
            <div className="category">{categoryLabel(result.categoria)}</div>
            <div className="confidence-row"><span>Confiança</span><strong>{Math.round(result.confianca * 100)}%</strong></div>
            <div className="progress"><span style={{ width: `${result.confianca * 100}%` }} /></div>
            <div className={`status ${result.status?.toLowerCase()}`}>{result.status || 'CLASSIFICADO'}</div>
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
                <option value="PENDENTE">Pendente</option>
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
        {artigoSelecionado && <div className="details-content">
          <div className="category-label">CATEGORIA</div>
          <div className="category">{categoryLabel(artigoSelecionado.categoria)}</div>
          <h3>{artigoSelecionado.titulo}</h3>
          <p className="details-text">{artigoSelecionado.texto}</p>
          <div className="details-grid">
            <div><span>Autores</span><strong>{artigoSelecionado.autores || '—'}</strong></div>
            <div><span>Ano</span><strong>{artigoSelecionado.ano || '—'}</strong></div>
            <div><span>Confiança</span><strong>{Math.round(artigoSelecionado.confianca * 100)}%</strong></div>
            <div><span>Status</span><strong>{artigoSelecionado.status}</strong></div>
            <div><span>Criado em</span><strong>{artigoSelecionado.criadoEm ? new Date(artigoSelecionado.criadoEm).toLocaleString('pt-BR') : '—'}</strong></div>
          </div>
          {artigoSelecionado.link && <p><a href={artigoSelecionado.link} target="_blank" rel="noreferrer">Abrir link original ↗</a></p>}
          <div className="keywords"><h3>Palavras-chave</h3><div>{(artigoSelecionado.palavrasChave || []).map((keyword) => <span key={keyword}>{keyword}</span>)}</div></div>
        </div>}
      </section>}
    </main>
  )
}

export default App
