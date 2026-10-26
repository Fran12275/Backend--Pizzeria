package org.model;

public class PagarEfectivo implements Pagable{

    @Override
    public void pagar(double cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad a pagar debe ser mayor que 0");
        }
        System.out.printf("Método de pago: EFECTIVO. El cliente entrega " + cantidad + "€ en metálico al recibir el pedido.");
    }
}
