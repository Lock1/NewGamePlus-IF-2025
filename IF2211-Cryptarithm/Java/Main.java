import java.io.IO;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.IntBinaryOperator;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;





enum Functionals {
    ;
    public static <T1,T2> LiftedOf2<T1,T2> liftArgs(Optional<T1> t1, Optional<T2> t2) {
        @SuppressWarnings({"unchecked", "rawtypes"}) // Thank you JLS for not allowing this
        LiftedOf2<T1,T2> res = (LiftedOf2) f -> t1.flatMap(v1 -> t2.map(v2 -> f.apply(v1, v2)));
        return res;
    }

    public static <T> T uncheckException(FailableSupplier<T> sup) {
        try {
            return sup.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private interface LiftedOf2<T1,T2> { <R> Optional<R> apply(BiFunction<? super T1,? super T2,R> f); }
    interface FailableSupplier<T> { T get() throws Exception; }
    public record Pair<T1,T2>(T1 left, T2 right) {}
}

sealed interface CryptarithmExpression {
    public record BoolExpression(CryptarithmExpression lhs, CryptarithmExpression rhs)     implements CryptarithmExpression {}
    public record AdditionExpression(CryptarithmExpression lhs, CryptarithmExpression rhs) implements CryptarithmExpression {}
    public @FunctionalInterface non-sealed interface EvaluableExpression                   extends    CryptarithmExpression {
        int evaluateUsingMap(final byte[] valueMap); // Contract: valueMap is guaranteed to cover byte[0..26] -> byte
    }
    public sealed interface CryptarithmValue extends CryptarithmExpression {
        record Boolean(boolean b) implements CryptarithmValue {};
        record Integer(int i)     implements CryptarithmValue {};
    }

    public static Optional<BoolExpression> construct(List<String> source) {
        assert source.size() >= 3 : "Explode";
        final BinaryOperator<AdditionExpression> reducer = (e1, e2) -> {
            if (e1 instanceof AdditionExpression(var e1Left, EvaluableExpression e1Right) && e2 instanceof AdditionExpression(var e2Left, _)) {
                return new AdditionExpression(e1Left, new AdditionExpression(e1Right, e2Left));
            } else if (e1 instanceof AdditionExpression(var e1Left, _) && e2 instanceof AdditionExpression(var e2Left, _)) {
                return new AdditionExpression(e1Left, e2Left);
            }
            throw new IllegalArgumentException("Explode");
        };

        Optional<EvaluableExpression> rhs = CryptarithmExpression.construct(source.getLast())
            .flatMap(e -> e instanceof EvaluableExpression expr ? Optional.of(expr) : Optional.empty());
        Optional<AdditionExpression> lhs = source.subList(0, source.size()-1).stream()
            .map(e -> CryptarithmExpression.construct(e))
            .map(e -> new AdditionExpression(e.orElseThrow(IllegalArgumentException::new), null))
            .reduce(reducer);
        return Functionals.liftArgs(lhs, rhs).apply((l, r) -> new BoolExpression(l, r));
    }

    public static Optional<EvaluableExpression> construct(String source) {
        enum LocalScope {
            ; // Uh okay, due to lambda-funcinterface mechanic, recursive inline funcinterface looks like prohibited
            public static int pow(int exp, int n) {
                return exp > 0 ? pow(exp-1, n*n) : 1;
            }
        }

        if (Objects.nonNull(source)) {
            byte[] reverseSource = Functionals.uncheckException(() -> new StringBuilder(source.toLowerCase()).reverse().toString().getBytes("US-ASCII"));
            EvaluableExpression expr = map -> IntStream.range(0, reverseSource.length)
                .map(i -> map[reverseSource[i]-'a'] * LocalScope.pow(i, 10))
                .reduce(0, Integer::sum);
            return Optional.of(expr);
        } else {
            return Optional.empty();
        }
    }

    default CryptarithmValue evaluate(final byte[] valueMap) {
        return switch (this) {
            case BoolExpression(AdditionExpression e1, EvaluableExpression e2)      -> {
                if (e1.evaluate(valueMap) instanceof CryptarithmValue.Integer(int v1))
                    yield new CryptarithmValue.Boolean(v1 == e2.evaluateUsingMap(valueMap));
                else
                    throw new IllegalArgumentException("Explode");
            }
            case AdditionExpression(EvaluableExpression e1, AdditionExpression e2)  -> {
                if (e2.evaluate(valueMap) instanceof CryptarithmValue.Integer(int v2)) 
                    yield new CryptarithmValue.Integer(e1.evaluateUsingMap(valueMap) + v2);
                else 
                    throw new IllegalArgumentException("Explode");
            }
            case AdditionExpression(EvaluableExpression e1, EvaluableExpression e2) -> new CryptarithmValue.Integer(
                e1.evaluateUsingMap(valueMap) + e2.evaluateUsingMap(valueMap)
            );
            case EvaluableExpression e                                              -> new CryptarithmValue.Integer(e.evaluateUsingMap(valueMap));
            default -> throw new IllegalArgumentException("Explode"); // C: This is bad
        };
    }

    default CryptarithmExpression partialEvaluate(final byte[] valueMap) {
        return switch (this) {
            case BoolExpression(AdditionExpression e1, EvaluableExpression e2)      -> {
                if (e1.evaluate(valueMap) instanceof CryptarithmValue.Integer v1)
                    yield new BoolExpression(v1, new CryptarithmValue.Integer(e2.evaluateUsingMap(valueMap)));
                else
                    throw new IllegalArgumentException("Explode");
            }
            case AdditionExpression(EvaluableExpression e1, AdditionExpression e2)  -> {
                if (e2.evaluate(valueMap) instanceof CryptarithmValue.Integer(int v2)) 
                    yield new CryptarithmValue.Integer(e1.evaluateUsingMap(valueMap) + v2);
                else 
                    throw new IllegalArgumentException("Explode");
            }
            case AdditionExpression(EvaluableExpression e1, EvaluableExpression e2) -> new CryptarithmValue.Integer(
                e1.evaluateUsingMap(valueMap) + e2.evaluateUsingMap(valueMap)
            );
            case EvaluableExpression e                                              -> new CryptarithmValue.Integer(e.evaluateUsingMap(valueMap));
            default -> throw new IllegalArgumentException("Explode"); // C: This is bad
        };
    }
}



enum BruteForce {
    ;

    public static void execute(List<String> source) {
        enum LocalScope {
            ;
            static Functionals.Pair<Stream<byte[]>,CryptarithmExpression.BoolExpression> preprocess(List<String> source) {
                List<String> preprocessed = source.stream().map(String::toLowerCase).toList();
                return new Functionals.Pair<>(
                    nonStrictStateSpaceEnumerator(generateUnique(preprocessed), new byte[26]),
                    CryptarithmExpression.construct(preprocessed).orElseThrow(IllegalArgumentException::new)
                );
            }

            static List<Byte> generateUnique(List<String> source) {
                return source.stream()
                    .map(String::toLowerCase)
                    .map(str -> Functionals.uncheckException(() -> str.getBytes("US-ASCII")))
                    .flatMap(arr -> IntStream.range(0, arr.length).mapToObj(i -> arr[i]))
                    .distinct()
                    .toList();
            }

            static Stream<byte[]> nonStrictStateSpaceEnumerator(List<Byte> possibleChars, byte[] acc) {
                if (!possibleChars.isEmpty()) {
                    byte c = possibleChars.getFirst();
                    List<Byte> leftover = possibleChars.size() > 1 ? possibleChars.subList(1, possibleChars.size()) : Collections.emptyList();
                    return IntStream.range(0, 10)
                        .mapToObj(i -> LocalScope.alterCopy(acc, c, (byte) i))
                        .flatMap(newAcc -> nonStrictStateSpaceEnumerator(leftover, newAcc));
                } else {
                    return Stream.of(acc);
                }
            }

            static byte[] alterCopy(byte[] acc, byte c, byte x) {
                byte[] result = new byte[26];
                for (int i = 0; i < acc.length; ++i)
                    result[i] = acc[i];
                result[c - 'a'] = x;
                return result;
            }
        }

        var pair = LocalScope.preprocess(source);
        var solutionCount = pair.left.filter(sol -> pair.right.evaluate(sol) instanceof CryptarithmExpression.CryptarithmValue.Boolean(boolean b) ? b : false)
            .map(Debugger::arraySucks)
            .collect(Collectors.counting());
        IO.println("Solution count: " + solutionCount);
    }



    enum Debugger {
        ;
        public static List<Functionals.Pair<String,Integer>> arraySucks(byte[] solution) {
            var result = new ArrayList<Functionals.Pair<String,Integer>>(26);
            for (int i = 0; i < 26; ++i)
                if (solution[i] != 0)
                    result.add(new Functionals.Pair<>(shutupEncoding((byte) ('a' + i)), Byte.toUnsignedInt(solution[i])));
            return result;
        }

        public static String shutupEncoding(byte[] bytes) {
            try {
                return new String(bytes, "US-ASCII");
            } catch (Exception _) { // Ah, unsupported encoding, the bane of my existence. Enum exist yet somehow prefer non-exhaustive String, thanks old Java designers
                throw new RuntimeException("Explode");
            }
        }

        public static String shutupEncoding(byte bite) {
            return shutupEncoding(new byte[] { bite });
        }

        public static String bytesToString(List<Byte> bytes) {
            byte[] result = new byte[bytes.size()];
            for (int i = 0; i < bytes.size(); ++i)
                result[i] = bytes.get(i);
            return shutupEncoding(result);
        }
    }
}



void main() {
    List<String> sampleProblem = List.of("COCA", "COLA", "OASIS");
    BruteForce.execute(sampleProblem);
}