package sort;

import model.Location;
import java.util.List;

public interface Sorter {
    String getName();
    void sort(List<Location> locations);
}
