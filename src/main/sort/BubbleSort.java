package sort;

import model.Location;
import java.util.List;

public class BubbleSort implements Sorter {

    @Override
    public String getName() {
        return "Bubble Sort";
    }

    @Override
    public void sort(List<Location> locations) {
        int n = locations.size();
        for (int i = 0; i < n - 1; i++) {
            boolean swapped = false;
            for (int j = 0; j < n - 1 - i; j++) {
                if (locations.get(j).compareTo(locations.get(j + 1)) > 0) {
                    Location temp = locations.get(j);
                    locations.set(j, locations.get(j + 1));
                    locations.set(j + 1, temp);
                    swapped = true;
                }
            }
            if (!swapped) {
                break;
            }
        }
    }
}
