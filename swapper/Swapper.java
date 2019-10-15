package swapper;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class Swapper<E> {

    private Set<E> set;

    public Swapper() {

        this.set = new HashSet<>();
    }

    private void removeAndAddElements(Collection<E> remove, Collection<E> add) throws InterruptedException {

        set.removeAll(remove);
        if (Thread.currentThread().isInterrupted())
            throw new InterruptedException("Thread was interrupted");

        set.addAll(add);
        if (Thread.currentThread().isInterrupted())
            throw new InterruptedException("Thread was interrupted");
    }

    private void fixSet(Collection<E> removed, Collection<E> added, Collection<E> intersectionWithAdded) {

        set.addAll(removed);
        set.removeAll(added);
        set.addAll(intersectionWithAdded);
    }

    public void swap(Collection<E> removed, Collection<E> added) throws InterruptedException {

        synchronized (this) {

            while (!set.containsAll(removed)) {
                wait();
            }

            Set<E> intersectionWithAdded = new HashSet<>(added);
            intersectionWithAdded.retainAll(set);

            try {

                removeAndAddElements(removed, added);
                notifyAll();

            } catch (InterruptedException e) {

                fixSet(added, removed, intersectionWithAdded);
                throw e;
            }
        }
    }
}
