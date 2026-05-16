package sort;

import model.Location;
import java.util.List;

public class QuickSort implements Sorter {

    @Override
    public String getName() {
        return "Quick Sort";
    }

    @Override
    public void sort(List<Location> locations) {
        quickSort(locations, 0, locations.size() - 1);
    }

    private void quickSort(List<Location> list, int low, int high) {
        if (low < high) {
            int pivotIndex = partition(list, low, high);
            quickSort(list, low, pivotIndex - 1);
            quickSort(list, pivotIndex + 1, high);
        }
    }

    private int partition(List<Location> list, int low, int high) {
        // Median-of-three pivot selection to reduce worst-case behavior
        int mid = low + (high - low) / 2;
        Location pivot = medianOfThree(list.get(low), list.get(mid), list.get(high));
        int pivotIdx;
        if (list.get(low).compareTo(pivot) == 0) {
            pivotIdx = low;
        } else if (list.get(mid).compareTo(pivot) == 0) {
            pivotIdx = mid;
        } else {
            pivotIdx = high;
        }
        swap(list, pivotIdx, high);

        pivot = list.get(high);
        int i = low - 1;
        for (int j = low; j < high; j++) {
            if (list.get(j).compareTo(pivot) <= 0) {
                i++;
                swap(list, i, j);
            }
        }
        swap(list, i + 1, high);
        return i + 1;
    }

    private Location medianOfThree(Location a, Location b, Location c) {
        if (a.compareTo(b) <= 0 && a.compareTo(c) <= 0) {
            return b.compareTo(c) <= 0 ? b : c;
        } else if (b.compareTo(a) <= 0 && b.compareTo(c) <= 0) {
            return a.compareTo(c) <= 0 ? a : c;
        } else {
            return a.compareTo(b) <= 0 ? a : b;
        }
    }

    private void swap(List<Location> list, int i, int j) {
        Location temp = list.get(i);
        list.set(i, list.get(j));
        list.set(j, temp);
    }
}
