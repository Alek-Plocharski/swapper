import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import swapper.Swapper;

public class CzytelnicyPisarze {

    private static final int CZYTELNICY = 10;
    private static final int PISARZE = 2;
    private static final Set<String> empty = new HashSet<>();
    private static final Set<String> ochrona = new HashSet<>();
    private static final Set<String> pisarze = new HashSet<>();
    private static final Set<String> czytelnicy = new HashSet<>();
    private static Swapper<String> mutex = new Swapper<>();
    private static int pisarzeIleCzeka = 0;
    private static int czytelnicyIleCzeka = 0;
    private static int pisarzeIlePisze = 0;
    private static int czytelnicyIleCzyta = 0;

    static {
        try {
            ochrona.add("ochrona");
            pisarze.add("pisarze");
            czytelnicy.add("czytelnicy");

            mutex.swap(empty, ochrona);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Wątek przerwany");
        }
    }

    private static class Czytelnik implements Runnable {

        @Override
        public void run() {

            try {
                while (true) {
                    mutex.swap(ochrona, empty);

                    if (pisarzeIlePisze + pisarzeIleCzeka > 0) {
                        czytelnicyIleCzeka++;
                        mutex.swap(empty, ochrona);
                        mutex.swap(czytelnicy, empty);
                        czytelnicyIleCzeka--;
                    }

                    czytelnicyIleCzyta++;
                    if (czytelnicyIleCzeka > 0) {
                        mutex.swap(empty, czytelnicy);
                    } else {
                        mutex.swap(empty, ochrona);
                    }

                    System.out.println("Zaczął czytać: " + Thread.currentThread().getName());
                    Thread.currentThread().sleep(100);
                    System.out.println("Skończył czytać: " + Thread.currentThread().getName());

                    mutex.swap(ochrona, empty);
                    czytelnicyIleCzyta--;
                    if (czytelnicyIleCzyta == 0 && pisarzeIleCzeka > 0) {
                        mutex.swap(empty, pisarze);
                    } else {
                        mutex.swap(empty, ochrona);
                    }
                }

            } catch (InterruptedException e) {
                Thread t = Thread.currentThread();
                t.interrupt();
                System.err.println(t.getName() + " przerwany");
            }
        }

    }

    private static class Pisarz implements Runnable {

        @Override
        public void run() {
            try {

                while (true) {

                    mutex.swap(ochrona, empty);
                    if (pisarzeIlePisze + czytelnicyIleCzyta > 0) {
                        pisarzeIleCzeka++;
                        mutex.swap(empty, ochrona);
                        mutex.swap(pisarze, empty);
                        pisarzeIleCzeka--;
                    }

                    pisarzeIlePisze++;
                    mutex.swap(empty, ochrona);

                    System.out.println("Zaczął pisać: " + Thread.currentThread().getName());
                    Thread.currentThread().sleep(100);
                    System.out.println("Skończył pisać: " + Thread.currentThread().getName());

                    mutex.swap(ochrona, empty);
                    pisarzeIlePisze--;
                    if (czytelnicyIleCzeka > 0) {
                        mutex.swap(empty, czytelnicy);
                    } else if (pisarzeIleCzeka > 0){
                        mutex.swap(empty, pisarze);
                    } else {
                        mutex.swap(empty, ochrona);
                    }
                }

            } catch (InterruptedException e) {
                Thread t = Thread.currentThread();
                t.interrupt();
                System.err.println(t.getName() + " przerwany");
            }
        }

    }

    public static void main(String args[]) {

        List<Thread> wątki = new ArrayList<>();
        for (int i = 0; i < CZYTELNICY; ++i) {
            Runnable r = new Czytelnik();
            Thread t = new Thread(r, "Czytelnik" + i);
            wątki.add(t);
        }
        for (int i = 0; i < PISARZE; ++i) {
            Runnable r = new Pisarz();
            Thread t = new Thread(r, "Pisarz" + i);
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
