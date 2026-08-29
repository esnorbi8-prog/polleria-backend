package com.polleria.pedidos.pago.dto;

/**
 * Cuerpo que envía Mercado Pago al webhook cuando ocurre un evento de pago.
 * Ejemplo real que manda Mercado Pago:
 *
 * <pre>{ "type": "payment", "data": { "id": "123456789" } }</pre>
 */
public class WebhookNotification {

    private String type;
    private String action;
    private Data data;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public static class Data {
        private String id;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }
}
