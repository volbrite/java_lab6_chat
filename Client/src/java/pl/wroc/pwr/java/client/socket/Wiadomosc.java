package pl.wroc.pwr.java.client.socket;

import java.io.Serializable;

public class Wiadomosc implements Serializable {

    private static final long serialVersionUID = 1L;
    public String typWiadomosci, wysylajacy, zawartosc, odbiorca;

    public Wiadomosc(String typWiadomosci, String wysylajacy, String zawartosc, String odbiorca) {
        this.typWiadomosci = typWiadomosci;
        this.wysylajacy = wysylajacy;
        this.zawartosc = zawartosc;
        this.odbiorca = odbiorca;
    }

    @Override
    public String toString() {
        return "{typWiadomosci='" + typWiadomosci + "', wysylajacy='" + wysylajacy + "', zawartosc='" + zawartosc + "', odbiorca='" + odbiorca + "'}";
    }
}
