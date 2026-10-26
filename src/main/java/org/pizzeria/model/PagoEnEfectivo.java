package org.pizzeria.model;

import java.util.Locale;

public class PagoEnEfectivo implements Pagable{

    @Override
    public void pagar(double cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad a pagar debe ser mayor que 0");
        }
        // Formato de España: el importe sale con coma decimal (17,00 €) en cualquier ordenador
        System.out.printf(Locale.of("es", "ES"), "Método de pago: EFECTIVO. El cliente entrega %.2f € en metálico al recibir el pedido.%n", cantidad);
    }
}
