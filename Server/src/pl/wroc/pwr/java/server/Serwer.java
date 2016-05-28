package pl.wroc.pwr.java.server;

import java.io.*;
import java.net.*;

import pl.wroc.pwr.java.client.socket.Wiadomosc;

class SerwerWatek extends Thread {

    public Serwer serwer = null;
    public Socket socket = null;
    public int ID = -1;
    public String nazwaUzytkownika = "abc";
    public ObjectInputStream streamIn = null;
    public ObjectOutputStream streamOut = null;
    public SerwerRamka gui;

    public SerwerWatek(Serwer _server, Socket _socket) {
        super();
        serwer = _server;
        socket = _socket;
        ID = socket.getPort();
        gui = _server.gui;
    }

    public void send(Wiadomosc msg) {
        try {
            streamOut.writeObject(msg);
            streamOut.flush();
        } catch (IOException ex) {
            System.out.println("Wyjątek <Klient : send(...)]");
        }
    }

    public int getID() {
        return ID;
    }

    @SuppressWarnings("deprecation")
    public void run() {
        gui.jTextArea1.append("\nWątek serwera o ID: " + ID + " został uruchomiony.");
        while (true) {
            try {
                Wiadomosc msg = (Wiadomosc) streamIn.readObject();
                serwer.obslugaWatku(ID, msg);
            } catch (Exception ioe) {
                System.out.println(ID + " Błąd odczytu: " + ioe.getMessage());
                ioe.printStackTrace();
                serwer.remove(ID);
                stop();
            }
        }
    }

    public void otworz() throws IOException {
        streamOut = new ObjectOutputStream(socket.getOutputStream());
        streamOut.flush();
        streamIn = new ObjectInputStream(socket.getInputStream());
    }

    public void zamknij() throws IOException {
        if (socket != null) socket.close();
        if (streamIn != null) streamIn.close();
        if (streamOut != null) streamOut.close();
    }
}

public class Serwer implements Runnable {

    public SerwerWatek klienci[];
    public ServerSocket serwer = null;
    public Thread watek = null;
    public int iloscKlientow = 0, port = 13000;
    public SerwerRamka gui;
    public BazaDanych db;

    public Serwer(SerwerRamka frame) {

        klienci = new SerwerWatek[50];
        gui = frame;
        db = new BazaDanych(gui.bazaDanychPath);

        try {
            serwer = new ServerSocket(port);
            port = serwer.getLocalPort();
            gui.jTextArea1.append("Uruchomiono serwer. IP: " + InetAddress.getLocalHost() + ", Port : " + serwer.getLocalPort());
            start();
        } catch (IOException ioe) {
            gui.jTextArea1.append("Nie można uruchomić serwera na porcie: " + port + "\nPonawiam próbę.");
            gui.RetryStart(0);
        }
    }

    public Serwer(SerwerRamka ramka, int port) {

        klienci = new SerwerWatek[50];
        gui = ramka;
        this.port = port;
        db = new BazaDanych(gui.bazaDanychPath);

        try {
            serwer = new ServerSocket(this.port);
            this.port = serwer.getLocalPort();
            gui.jTextArea1.append("Uruchomiono serwer. IP: " + InetAddress.getLocalHost() + ", Port : " + serwer.getLocalPort());
            start();
        } catch (IOException ioe) {
            gui.jTextArea1.append("\nNie można uruchomić serwera na porcie: " + this.port + ": " + ioe.getMessage());
        }
    }

    public void run() {
        while (watek != null) {
            try {
                gui.jTextArea1.append("\nOczekiwanie na klienta...");
                dodajWatek(serwer.accept());
            } catch (Exception ioe) {
                gui.jTextArea1.append("\nBłąd: \n");
                gui.RetryStart(0);
            }
        }
    }

    public void start() {
        if (watek == null) {
            watek = new Thread(this);
            watek.start();
        }
    }

    @SuppressWarnings("deprecation")
    public void stop() {
        if (watek != null) {
            watek.stop();
            watek = null;
        }
    }

    private int znajdzKlienta(int ID) {
        for (int i = 0; i < iloscKlientow; i++) {
            if (klienci[i].getID() == ID) {
                return i;
            }
        }
        return -1;
    }

    public synchronized void obslugaWatku(int ID, Wiadomosc msg) {
        if (msg.zawartosc.equals("<zegnaj>")) {
            powiadomWszystkich("wyloguj", "SERWER", msg.wysylajacy);
            remove(ID);
        } else {
            if (msg.typWiadomosci.equals("zaloguj")) {
                if (znajdzWatekUzytkownika(msg.wysylajacy) == null) {
                    if (db.sprawdzLogin(msg.wysylajacy, msg.zawartosc)) {
                        klienci[znajdzKlienta(ID)].nazwaUzytkownika = msg.wysylajacy;
                        klienci[znajdzKlienta(ID)].send(new Wiadomosc("zaloguj", "SERWER", "TRUE", msg.wysylajacy));
                        powiadomWszystkich("newuser", "SERVER", msg.wysylajacy);
                        wyslijNowaListeUzytkownikow(msg.wysylajacy);
                    } else {
                        klienci[znajdzKlienta(ID)].send(new Wiadomosc("zaloguj", "SERWER", "FALSE", msg.wysylajacy));
                    }
                } else {
                    klienci[znajdzKlienta(ID)].send(new Wiadomosc("zaloguj", "SERWER", "FALSE", msg.wysylajacy));
                }
            } else if (msg.typWiadomosci.equals("wiadomosc")) {
                if (msg.odbiorca.equals("Wszyscy")) {
                    powiadomWszystkich("wiadomosc", msg.wysylajacy, msg.zawartosc);
                } else {
                    znajdzWatekUzytkownika(msg.odbiorca).send(new Wiadomosc(msg.typWiadomosci, msg.wysylajacy, msg.zawartosc, msg.odbiorca));
                    klienci[znajdzKlienta(ID)].send(new Wiadomosc(msg.typWiadomosci, msg.wysylajacy, msg.zawartosc, msg.odbiorca));
                }
            } else if (msg.typWiadomosci.equals("test")) {
                klienci[znajdzKlienta(ID)].send(new Wiadomosc("test", "SERWER", "OK", msg.wysylajacy));
            } else if (msg.typWiadomosci.equals("zarejestrujUzytkownika")) {
                if (znajdzWatekUzytkownika(msg.wysylajacy) == null) {
                    if (!db.uzytkownikIstnieje(msg.wysylajacy)) {
                        db.dodajUzytkownika(msg.wysylajacy, msg.zawartosc);
                        klienci[znajdzKlienta(ID)].nazwaUzytkownika = msg.wysylajacy;
                        klienci[znajdzKlienta(ID)].send(new Wiadomosc("zarejestrujUzytkownika", "SERWER", "TRUE", msg.wysylajacy));
                        klienci[znajdzKlienta(ID)].send(new Wiadomosc("zaloguj", "SERWER", "TRUE", msg.wysylajacy));
                        powiadomWszystkich("nowyUzytkownik", "SERWER", msg.wysylajacy);
                        wyslijNowaListeUzytkownikow(msg.wysylajacy);
                    } else {
                        klienci[znajdzKlienta(ID)].send(new Wiadomosc("zarejestrujUzytkownika", "SERWER", "FALSE", msg.wysylajacy));
                    }
                } else {
                    klienci[znajdzKlienta(ID)].send(new Wiadomosc("zarejestrujUzytkownika", "SERWER", "FALSE", msg.wysylajacy));
                }
            } else if (msg.typWiadomosci.equals("upload_req")) {
                if (msg.odbiorca.equals("All")) {
                    klienci[znajdzKlienta(ID)].send(new Wiadomosc("message", "SERVER", "Uploading to 'All' forbidden", msg.wysylajacy));
                } else {
                    znajdzWatekUzytkownika(msg.odbiorca).send(new Wiadomosc("upload_req", msg.wysylajacy, msg.zawartosc, msg.odbiorca));
                }
            } else if (msg.typWiadomosci.equals("upload_res")) {
                if (!msg.zawartosc.equals("NO")) {
                    String IP = znajdzWatekUzytkownika(msg.wysylajacy).socket.getInetAddress().getHostAddress();
                    znajdzWatekUzytkownika(msg.odbiorca).send(new Wiadomosc("upload_res", IP, msg.zawartosc, msg.odbiorca));
                } else {
                    znajdzWatekUzytkownika(msg.odbiorca).send(new Wiadomosc("upload_res", msg.wysylajacy, msg.zawartosc, msg.odbiorca));
                }
            }
        }
    }

    public void powiadomWszystkich(String type, String sender, String content) {
        Wiadomosc msg = new Wiadomosc(type, sender, content, "Wszyscy");
        for (int i = 0; i < iloscKlientow; i++) {
            klienci[i].send(msg);
        }
    }

    public void wyslijNowaListeUzytkownikow(String doKogo) {
        for (int i = 0; i < iloscKlientow; i++) {
            znajdzWatekUzytkownika(doKogo).send(new Wiadomosc("nowyUzytkownik", "SERWER", klienci[i].nazwaUzytkownika, doKogo));
        }
    }

    public SerwerWatek znajdzWatekUzytkownika(String usr) {
        for (int i = 0; i < iloscKlientow; i++) {
            if (klienci[i].nazwaUzytkownika.equals(usr)) {
                return klienci[i];
            }
        }
        return null;
    }

    @SuppressWarnings("deprecation")
    public synchronized void remove(int ID) {
        int pos = znajdzKlienta(ID);
        if (pos >= 0) {
            SerwerWatek doUsuniecia = klienci[pos];
            gui.jTextArea1.append("\nUsuwanie wątku klienta o ID: " + ID + " z pozycji: " + pos);
            if (pos < iloscKlientow - 1) {
                for (int i = pos + 1; i < iloscKlientow; i++) {
                    klienci[i - 1] = klienci[i];
                }
            }
            iloscKlientow--;
            try {
                doUsuniecia.zamknij();
            } catch (IOException ioe) {
                gui.jTextArea1.append("\nBłąd podczas zamykania wątku: " + ioe.getMessage());
            }
            doUsuniecia.stop();
        }
    }

    private void dodajWatek(Socket socket) {
        if (iloscKlientow < klienci.length) {
            gui.jTextArea1.append("\nZaakceptowano połączenie z klientem: " + socket);
            klienci[iloscKlientow] = new SerwerWatek(this, socket);
            try {
                klienci[iloscKlientow].otworz();
                klienci[iloscKlientow].start();
                iloscKlientow++;
            } catch (IOException ioe) {
                gui.jTextArea1.append("\nBłąd otwarcia wątku: " + ioe.getMessage());
            }
        } else {
            gui.jTextArea1.append("\nKlient odrzucony: maksymalna ilość użytkowników (" + klienci.length + ") została osiągnięta.");
        }
    }
}
