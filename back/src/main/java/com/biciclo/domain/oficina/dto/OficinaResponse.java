package com.biciclo.domain.oficina.dto;

/**
 * Oficina/loja de bicicletas vinda do Google Places, em tempo real.
 * Pelos termos do Google, estes dados não são gravados no Firestore.
 * O front-end deve exibir a atribuição "Google Maps" junto da lista.
 *
 * @param placeId    id do lugar no Google (único dado que pode ser guardado)
 * @param abertoAgora null quando o Google não informa o horário
 */
public record OficinaResponse(
        String placeId,
        String nome,
        String endereco,
        Double latitude,
        Double longitude,
        Double nota,
        Integer totalAvaliacoes,
        Boolean abertoAgora,
        String googleMapsUrl
) {
}
