package pl.wroc.pwr.java.client.socket;

import pl.wroc.pwr.java.client.gui.CzatRamka;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Date;

public class Klient implements Runnable {

    public int port;
    public String serverAddr;
    public Socket socket;
    public CzatRamka gui;
    public ObjectInputStream In;
    public ObjectOutputStream Out;
    public Historia hist;

    public Klient(CzatRamka frame) throws IOException {
        gui = frame;
        this.serverAddr = gui.adresSerwera;
        this.port = gui.port;
        socket = new Socket(InetAddress.getByName(serverAddr), port);

        Out = new ObjectOutputStream(socket.getOutputStream());
        Out.flush();
        In = new ObjectInputStream(socket.getInputStream());

        hist = gui.hist;
    }

    @Override
    public void run() {
        boolean keepRunning = true;
        while (keepRunning) {
            try {
                Wiadomosc msg = (Wiadomosc) In.readObject();
                System.out.println("Przychodzący: " + msg.toString());

                if (msg.typWiadomosci.equals("wiadomosc")) {
                    if (msg.odbiorca.equals(gui.nazwaUzytkownika)) {
                        gui.jTextArea1.append("<" + msg.wysylajacy + " => " + gui.nazwaUzytkownika + " > : " + msg.zawartosc + "\n");
                    } else {
                        gui.jTextArea1.append("<" + msg.wysylajacy + " => " + msg.odbiorca + "> : " + msg.zawartosc + "\n");
                    }

                    if (!msg.zawartosc.equals("<zegnaj>") && !msg.wysylajacy.equals(gui.nazwaUzytkownika)) {
                        String msgTime = (new Date()).toString();

                        try {
                            hist.addMessage(msg, msgTime);
                            DefaultTableModel table = (DefaultTableModel) gui.historiaRamka.jTable1.getModel();
                            table.addRow(new Object[]{msg.wysylajacy, msg.zawartosc, gui.nazwaUzytkownika, msgTime});
                        } catch (Exception ex) {
                        }
                    }
                } else if (msg.typWiadomosci.equals("zaloguj")) {
                    if (msg.zawartosc.equals("TRUE")) {
                        gui.jButton2.setEnabled(false);
                        gui.jButton3.setEnabled(false);
                        gui.jButton4.setEnabled(true);
                        gui.jButton5.setEnabled(true);
                        gui.jTextArea1.append("[SERWER > " + gui.nazwaUzytkownika + "] : Zalogowano poprawnie\n");
                        gui.jTextField3.setEnabled(false);
                        gui.jPasswordField1.setEnabled(false);
                    } else {
                        gui.jTextArea1.append("[SERWER > " + gui.nazwaUzytkownika + "] : Logowanie nieudane\n");
                    }
                } else if (msg.typWiadomosci.equals("test")) {
                    gui.jButton1.setEnabled(false);
                    gui.jButton2.setEnabled(true);
                    gui.jButton3.setEnabled(true);
                    gui.jTextField3.setEnabled(true);
                    gui.jPasswordField1.setEnabled(true);
                    gui.jTextField1.setEditable(false);
                    gui.jTextField2.setEditable(false);
                    gui.jButton7.setEnabled(true);
                } else if (msg.typWiadomosci.equals("nowyUzytkownik")) {
                    if (!msg.zawartosc.equals(gui.nazwaUzytkownika)) {
                        boolean exists = false;
                        for (int i = 0; i < gui.model.getSize(); i++) {
                            if (gui.model.getElementAt(i).equals(msg.zawartosc)) {
                                exists = true;
                                break;
                            }
                        }
                        if (!exists) {
                            gui.model.addElement(msg.zawartosc);
                        }
                    }
                } else if (msg.typWiadomosci.equals("zarejestrujUzytkownika")) {
                    if (msg.zawartosc.equals("TRUE")) {
                        gui.jButton2.setEnabled(false);
                        gui.jButton3.setEnabled(false);
                        gui.jButton4.setEnabled(true);
                        gui.jButton5.setEnabled(true);
                        gui.jTextArea1.append("[SERWER => " + gui.nazwaUzytkownika + "] : Pomyślnie zarejestrowano.\n");
                    } else {
                        gui.jTextArea1.append("[SERWER => " + gui.nazwaUzytkownika + "] : Rejestracja nieudana.\n");
                    }
                } else if (msg.typWiadomosci.equals("wyloguj")) {
                    if (msg.zawartosc.equals(gui.nazwaUzytkownika)) {
                        gui.jTextArea1.append("[" + msg.wysylajacy + " => " + gui.nazwaUzytkownika + "] : Żegnaj\n");
                        gui.jButton1.setEnabled(true);
                        gui.jButton4.setEnabled(false);
                        gui.jTextField1.setEditable(true);
                        gui.jTextField2.setEditable(true);

                        for (int i = 1; i < gui.model.size(); i++) {
                            gui.model.removeElementAt(i);
                        }

                        gui.watekKlienta.stop();
                    } else {
                        gui.model.removeElement(msg.zawartosc);
                        gui.jTextArea1.append("<" + msg.wysylajacy + " => Wszyscy> : " + msg.zawartosc + " wylogował się.\n");
                    }

                } else {
                    gui.jTextArea1.append("<SERWER => " + gui.nazwaUzytkownika + "> : Nie rozpoznano typWiadomosci\n");
                }
            } catch (Exception ex) {
                keepRunning = false;
                ex.printStackTrace();
                gui.jTextArea1.append("<JAVA-V-Comm => " + gui.nazwaUzytkownika + "> : Connection Failure\n");
                gui.jButton1.setEnabled(true);
                gui.jTextField1.setEditable(true);
                gui.jTextField2.setEditable(true);
                gui.jButton4.setEnabled(false);
                gui.jButton5.setEnabled(false);
                gui.jButton5.setEnabled(false);

                for (int i = 1; i < gui.model.size(); i++) {
                    gui.model.removeElementAt(i);
                }

                gui.watekKlienta.stop();

                System.out.println("Wyjątek w Klient run()");
                ex.printStackTrace();
            }
        }
    }

    public void send(Wiadomosc msg) {
        try {
            Out.writeObject(msg);
            Out.flush();
            System.out.println("Wychodząca : " + msg.toString());

            if (msg.typWiadomosci.equals("wiadomosc") && !msg.zawartosc.equals("<zegnaj>")) {
                String msgTime = (new Date()).toString();
                try {
                    hist.addMessage(msg, msgTime);
                    DefaultTableModel table = (DefaultTableModel) gui.historiaRamka.jTable1.getModel();
                    table.addRow(new Object[]{gui.nazwaUzytkownika, msg.zawartosc, msg.odbiorca, msgTime});
                } catch (Exception ex) {
                }
            }
        } catch (IOException ex) {
            System.out.println("Wyjątek w Klient send()");
            ex.printStackTrace();
        }
    }

    public void closeThread(Thread t) {
        t = null;
    }
}
