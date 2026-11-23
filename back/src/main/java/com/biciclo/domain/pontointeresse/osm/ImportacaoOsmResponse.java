package com.biciclo.domain.pontointeresse.osm;

/**
 * Resumo de uma importação do OpenStreetMap.
 *
 * @param recebidos   elementos devolvidos pela Overpass
 * @param criados     POIs novos
 * @param atualizados POIs já importados antes e atualizados
 * @param ignorados   sem tipo reconhecido ou desativados por um ADMIN
 */
public record ImportacaoOsmResponse(
        int recebidos,
        int criados,
        int atualizados,
        int ignorados
) {
}
