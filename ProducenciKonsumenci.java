import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import swapper.Swapper;

public class ProducenciKonsumenci {

    private static final int PRODUCENCI = 100;
    private static final int PRODUKOWANE = 1000;
    private static final int KONSUMENCI = 50;
    private static final int KONSUMOWANE = (PRODUCENCI * PRODUKOWANE) / KONSUMENCI;
    private static final int ROZMIAR = 200;
    private static final int[] bufor = new int[ROZMIAR];
    private static final Set<String> empty = new HashSet<>();
    private static final Set<String> produkuj = new HashSet<>();
    private static final Set<String> konsumuj = new HashSet<>();
    private static final Set<String> ochrona = new HashSet<>();
    private static int ileProduktów = 0;
    private static int następneWolne = 0;
    private static int następneZajęte = 0;
    private static Swapper<String> mutex = new Swapper<>();

    private static void put(int x) throws InterruptedException {

        mutex.swap(produkuj, empty);

        bufor[następneWolne] = x; //nie trzeba ochrony na następne wolne, bo produkuje jeden na raz
        następneWolne = (następneWolne + 1) % ROZMIAR;

        mutex.swap(ochrona, empty);

        ileProduktów++;
        if (ileProduktów < ROZMIAR) { //budzimy producenta tylko jak jest miejsce
            mutex.swap(empty, produkuj);
        }
        if (ileProduktów == 1) { //obódź konsumenta, bo jest znowu co konsumować
            mutex.swap(empty, konsumuj);
        }

        mutex.swap(empty, ochrona);
    }

    private static int get() throws InterruptedException {

        mutex.swap(konsumuj, empty);

        int x = bufor[następneZajęte]; //nie trzeba ochrony na następne wolne, bo konsumuje jeden na raz
        następneZajęte = (następneZajęte + 1) % ROZMIAR;

        mutex.swap(ochrona, empty);

        ileProduktów--;
        if (ileProduktów > 0) { //są jeszcze produkty, czyli możemy obudzić kolejnego konsumenta
            mutex.swap(empty, konsumuj);
        }
        if (ileProduktów == ROZMIAR - 1) { //obódź producenta, bo znowu jest miejsce do produkowania
            mutex.swap(empty, produkuj);
        }

        mutex.swap(empty, ochrona);
        return x;
    }

    private static class Producent implements Runnable {

        private final int mój;

        public Producent(int mój) {
            this.mój = mój;
        }

        @Override
        public void run() {
            try {
                for (int i = 0; i < PRODUKOWANE; i++) {
                    put(mój);
                }
            } catch (InterruptedException e) {
                Thread t = Thread.currentThread();
                t.interrupt();
                System.err.println(t.getName() + " przerwany");
            }
        }
    }

    private static class Konsument implements Runnable {

        @Override
        public void run() {
            Thread t = Thread.currentThread();
            try {
                int suma = 0;
                int pobrane = 0;
                for (int i = 0; i < KONSUMOWANE; ++i) {
                    int x = get();
                    suma += x;
                    pobrane++;
                }
                System.out.println(t.getName() + " pobrał: " + pobrane + ", suma: " + suma);
            } catch (InterruptedException e) {
                t.interrupt();
                System.err.println(t.getName() + " przerwany");
            }
        }
    }

    public static void main(String args[]) {

        produkuj.add("Produkuj");
        konsumuj.add("Konsumuj");
        ochrona.add("Ochrona");

        try {
            mutex.swap(empty, produkuj);
            mutex.swap(empty, ochrona);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Główny przerwany");
        }

        List<Thread> wątki = new ArrayList<>();
        for (int i = 0; i < PRODUCENCI; ++i) {
            Runnable r = new Producent(2);
            Thread t = new Thread(r, "Producent" + i);
            wątki.add(t);
        }
        for (int i = 0; i < KONSUMENCI; ++i) {
            Runnable r = new Konsument();
            Thread t = new Thread(r, "Konsument" + i);
            wątki.add(t);
        }
        for (Thread t : wątki) {
            t.start();
        }
        try {
            for (Thread t : wątki) {
                t.join();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Główny przerwany");
        }
    }
}
