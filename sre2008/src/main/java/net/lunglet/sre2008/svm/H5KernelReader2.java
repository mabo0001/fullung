package net.lunglet.sre2008.svm;

import java.util.Arrays;
import net.lunglet.array4j.matrix.packed.FloatPackedMatrix;
import net.lunglet.array4j.matrix.packed.PackedFactory;
import net.lunglet.hdf.DataSet;
import net.lunglet.hdf.DataSpace;
import net.lunglet.hdf.H5File;
import net.lunglet.io.HDFReader;

/**
 * Kernel reader that reads the whole kernel up front.
 *
 * @author albert
 */
public final class H5KernelReader2 implements KernelReader {
    private final FloatPackedMatrix kernel;

    private final int[] orderLut;

    public H5KernelReader2(final H5File kernelh5) {
        DataSet orderds = kernelh5.getRootGroup().openDataSet("/order");
        DataSpace space = orderds.getSpace();
        int[] order = new int[(int) space.getDim(0)];
        space.close();
        orderds.read(order);
        orderds.close();
        int maxOrder = -1;
        for (int i : order) {
            maxOrder = i > maxOrder ? i : maxOrder;
        }
        this.orderLut = new int[maxOrder + 1];
        Arrays.fill(orderLut, -1);
        for (int i = 0; i < order.length; i++) {
            orderLut[order[i]] = i;
        }
        // use heap storage to avoid large direct memory allocations
        this.kernel = PackedFactory.floatSymmetricHeap(order.length);
        new HDFReader(kernelh5).read("/kernel", kernel);
    }

    public float read(final int i, final int j) {
        return kernel.get(orderLut[i], orderLut[j]);
    }

    public FloatPackedMatrix read(final int[] indexes) {
        int dim = indexes.length;
        FloatPackedMatrix subkernel = PackedFactory.floatSymmetricDirect(dim);
        for (int i = 0; i < indexes.length; i++) {
            for (int j = i; j < indexes.length; j++) {
                subkernel.set(i, j, read(indexes[i], indexes[j]));
            }
        }
        return subkernel;
    }
}
