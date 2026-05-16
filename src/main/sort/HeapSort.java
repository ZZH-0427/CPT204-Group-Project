package sort;

import model.Location;
import java.util.List;

public class HeapSort implements Sorter {

    @Override
    public String getName() {
        return "Heap Sort";
    }

    @Override
    public void sort(List<Location> locations) {
        int n = locations.size();

        for (int i = n / 2 - 1; i >= 0; i--) {
            heapify(locations, n, i);
        }

        for (int end = n - 1; end > 0; end--) {
            swap(locations, 0, end);
            heapify(locations, end, 0);
        }
    }

    private void heapify(List<Location> list, int heapSize, int rootIndex) {
        int largest = rootIndex;
        int left = 2 * rootIndex + 1;
        int right = 2 * rootIndex + 2;

        if (left < heapSize && list.get(left).compareTo(list.get(largest)) > 0) {
            largest = left;
        }

        if (right < heapSize && list.get(right).compareTo(list.get(largest)) > 0) {
            largest = right;
        }

        if (largest != rootIndex) {
            swap(list, rootIndex, largest);
            heapify(list, heapSize, largest);
        }
    }

    private void swap(List<Location> list, int i, int j) {
        Location temp = list.get(i);
        list.set(i, list.get(j));
        list.set(j, temp);
    }
}
