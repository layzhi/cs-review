package com.mmnaseri.cs.clrs.ch18.s2;

import com.mmnaseri.cs.clrs.ch18.s1.*;
import com.mmnaseri.cs.clrs.ch18.s3.BTreeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * @author Mohammad Milad Naseri (m.m.naseri@gmail.com)
 * @since 1.0 (7/28/15)
 */
public class ExpandableBTreeTest {

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testCreatingSmallDegree() throws Exception {
        new NonDeletingBTree(null, null, 1);
    }

    @DataProvider
    public Object[][] stressDataProvider() {
        final List<String> input = new ArrayList<>();
        final List<String> seed = Arrays.asList("1", "3", "2");
        for (String prefix : seed) {
            input.add(prefix);
        }
        for (int i = 0; i < 5; i++) {
            final List<List<String>> addendum = new ArrayList<>();
            for (String word : input) {
                final List<String> variations = new ArrayList<>();
                for (String suffix : seed) {
                    variations.add(word + suffix);
                }
                addendum.add(variations);
            }
            for (List<String> derivatives : addendum) {
                for (String derivative : derivatives) {
                    if (!input.contains(derivative)) {
                        input.add(derivative);
                    }
                }
            }
        }
        int fromDegree = 2;
        int toDegree = 250;
        int benchmark = 500;
        final List<Object[]> cases = new ArrayList<>();
        for (int degree = fromDegree; degree < toDegree + 1; degree++) {
            cases.add(new Object[]{degree, benchmark, input});
        }
        return cases.toArray(new Object[cases.size()][]);
    }

    @Test(dataProvider = "stressDataProvider")
    public void stressTest(int degree, int benchmark, List<String> input) throws Exception {
        for (int size = degree; size < benchmark; size++) {
            final MapStorage<ReflectiveIndexed<String>> dataStore = new MapStorage<>();
            final MapStorage<NodeDefinition<String>> nodeStore = new MapStorage<>();
            final NonDeletingBTree tree = new NonDeletingBTree(dataStore, nodeStore, degree);
            for (int insertion = 0; insertion < size; insertion++) {
                tree.insert(new ReflectiveIndexed<>(input.get(insertion)));
            }
            //each node must have a minimum of t-1 and a maximum of 2t-1 keys
            check(tree.getRoot(), degree);
            //all the items inserted in the tree must be available via lookup
            for (int index = 0; index < size; index++) {
                final ReflectiveIndexed<String> found = tree.find(new ReflectiveIndexed<>(input.get(index)));
                assertThat(found, is(notNullValue()));
                assertThat(found.getKey(), is(notNullValue()));
                assertThat(found.getKey(), is(input.get(index)));
            }
            //the items not inserted must not be found
            for (int index = size; index < input.size(); index++) {
                assertThat(tree.find(input.get(index)), is(nullValue()));
            }
            //the leaves in the tree must have the exact number of data that were inserted
            final List<String> trial = new ArrayList<>();
            collectLeaves(dataStore, tree.getRoot(), trial);
            assertThat(trial.size(), is(size));
            //the dat collected from leaves must come in sorted order
            final List<String> inserted = input.subList(0, size);
            Collections.sort(inserted);
            assertThat(trial, is(inserted));
        }
    }

    private static void collectLeaves(Storage<?> dataStore, BTreeNode<?, ?> node, List<String> collection) {
        if (node.isLeaf()) {
            int size = collection.size();
            final List<?> keys = node.getKeys();
            if (dataStore.read(node.getId(), 0) != null) {
                collection.add(String.valueOf(dataStore.read(node.getId(), 0)));
            } else {
                size--;
            }
            for (int i = 0; i < keys.size(); i++) {
                final Object value = dataStore.read(node.getId(), i + 1);
                if (value == null) {
                    size--;
                    continue;
                }
                collection.add(String.valueOf(value));
            }
            assertThat(collection.size() - size, is(keys.size() + 1));
        }
        node = node.getFirstChild();
        while (node != null) {
            collectLeaves(dataStore, node, collection);
            node = node.getNextSibling();
        }
    }

    private static class NonDeletingBTree extends ExpandableBTree<ReflectiveIndexed<String>, String> {

        public NonDeletingBTree(Storage<ReflectiveIndexed<String>> dataStore, Storage<NodeDefinition<String>> nodeStore, int degree) {
            super(dataStore, nodeStore, degree);
        }

        @Override
        public void delete(ReflectiveIndexed<String> value) {
            throw new UnsupportedOperationException();
        }

    }

    private static void check(BTreeNode node, int degree) {
        if (node.isRoot()) {
            assertThat(node.getKeys().isEmpty(), is(false));
        } else {
            assertThat(node.getKeys().size(), is(greaterThanOrEqualTo(degree - 1)));
            assertThat(node.getKeys().size(), is(lessThan(2 * degree)));
        }
        node = node.getFirstChild();
        while (node != null) {
            check(node, degree);
            node = node.getNextSibling();
        }
    }

}