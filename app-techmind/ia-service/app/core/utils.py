import re

def limpar_texto(texto: str) -> str:
    """Sanitiza o texto de entrada removendo quebras de linha e espaços extras."""
    if not texto:
        return ""
    texto_limpo = re.sub(r'[\r\n\t]+', ' ', texto)
    texto_limpo = re.sub(r'\s+', ' ', texto_limpo)
    return texto_limpo.strip()