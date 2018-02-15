package it.kdde.sequentialpatterns.fast;


import it.kdde.sequentialpatterns.model.SparseIdList;
import util.Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * this containsItemset the representation of a dataset for Fast
 */
public class FastDataset {

    public static final String ITEMSET_SEPARATOR = "-1";
    public static final String SEQUENCE_SEPARATOR = "-2";

    /**
     * Associates to each frequent itemset its SparseIdList
     */
    private Map<String, SparseIdList> itemSILMap;
    private final long numRows;
    private final float minSup;
    private int absMinSup;

    /**
     * @param numRows
     * @param minSup
     */
    private FastDataset(long numRows, float minSup) {
        this.itemSILMap = new HashMap<>();
        this.numRows = numRows;
        this.minSup = minSup;
        absMinSup = Utils.absoluteSupport(minSup, numRows);
        if (absMinSup == 0)
            absMinSup = 1;
    }

    /**
     * Finds all frequent 1 items
     */
    private void computeFrequentItems() {
        final Map<String, SparseIdList> newMap = new TreeMap<>();
        itemSILMap.forEach((item, sparseIdList) -> {
            if (sparseIdList.getAbsoluteSupport() >= absMinSup)
                newMap.put(item, sparseIdList);
        });
        itemSILMap = newMap;
    }

    public Map<String, SparseIdList> getFrequentItemsets() {
        return itemSILMap;
    }

    /**
     * Get the SparseIdList for a particular item
     *
     * @param item
     * @return a SparseIdList, return null if that SparseIdList doesn't exist in
     * dataset
     */
    public SparseIdList getSparseIdList(String item) {
        return itemSILMap.get(item);
    }


    public long getNumRows() {
        return numRows;
    }

    /**
     * "to change with abs count"
     *
     * @return
     */
    @Deprecated
    public float getMinSup() {
        return minSup;
    }

    public int getAbsMinSup() {
        return absMinSup;
    }

    /**
     * @param path
     * @param relativeSupport
     * @return
     */
    public static FastDataset fromPrefixspanSource(Path path, float relativeSupport) throws IOException {
        long numRows = Files.lines(path).count();
        final FastDataset fastDataset = new FastDataset(numRows, relativeSupport);

        int lineNumber = 0;
        String line;
        BufferedReader in = Files.newBufferedReader(path);
        while ((line = in.readLine()) != null) {

            if (line.length() == 0)
                continue;

            int transID = 1;

            StringTokenizer tokenizer = new StringTokenizer(line);
            String token;
            while (tokenizer.hasMoreElements()) {
                token = tokenizer.nextToken();

                if (token.equals(ITEMSET_SEPARATOR)) {
                    transID++;
                    continue;
                }

                if (token.equals(SEQUENCE_SEPARATOR))
                    break;

                SparseIdList inserted = fastDataset.itemSILMap.putIfAbsent(token, new SparseIdList((int) numRows));
                fastDataset.itemSILMap.get(token).addElement(lineNumber, transID);
            }
            lineNumber++;
        }
        fastDataset.computeFrequentItems();
        return fastDataset;
    }

    /**
     * @param path
     * @return
     * @throws IOException
     */
    private static long countNumRowsSpamSource(Path path) throws IOException {
        Set<String> custIds = Files.lines(path).
                filter(l -> l.length() > 0).
                map(l -> l.split(" ")[0]).collect(Collectors.toSet());

        return custIds.size();

    }

    /**
     *
     * @param path
     * @param relativeSupport
     * @return
     * @throws IOException
     */
    public static FastDataset fromSpamSource(Path path, float relativeSupport) throws IOException {

        long numRows = countNumRowsSpamSource(path);
        final FastDataset fastDataset = new FastDataset(numRows, relativeSupport);

        Files.lines(path).filter(l -> l.length() > 0).forEach(l -> {
            String[] split = l.split(" ");
            int custId = Integer.parseInt(split[0]);
            int transId = Integer.parseInt(split[1]);

            SparseIdList inserted = fastDataset.itemSILMap.putIfAbsent(split[2], new SparseIdList((int) numRows));
            inserted.addElement(custId,transId);
        });
        fastDataset.computeFrequentItems();
        return fastDataset;
    }
}
