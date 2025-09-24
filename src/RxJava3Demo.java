import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class RxJava3Demo {
    public static void main(String[] args) throws InterruptedException {
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        demo1_BasicOperations(numbers);

        demo2_AsyncProcessing(numbers);

        demo3_ParallelProcessing(numbers);

        demo4_CombiningStreams();

        demo5_PerformanceTest();
    }

    // 1. Базові операції
    private static void demo1_BasicOperations(List<Integer> numbers) {
        System.out.println("1. БАЗОВІ ОПЕРАЦІЇ З OBSERVABLE:");

        Observable.fromIterable(numbers)
                .filter(n -> {
                    System.out.println("Фільтрація: " + n + " в потоці: " + Thread.currentThread().getName());
                    return n % 2 == 0;
                })
                .map(n -> {
                    System.out.println("Квадратування: " + n + " в потоці: " + Thread.currentThread().getName());
                    return n * n;
                })
                .subscribe(
                        result -> System.out.println("Результат: " + result),
                        error -> System.err.println("Помилка: " + error)
                );
    }

    // 2. Асинхронна обробка
    private static void demo2_AsyncProcessing(List<Integer> numbers) {
        System.out.println("\n2. АСИНХРОННА ОБРОБКА З SCHEDULERS:");

        Observable.fromIterable(numbers)
                .subscribeOn(Schedulers.computation())
                .filter(n -> {
                    System.out.println("Асинхронна фільтрація: " + n + " в потоці: " + Thread.currentThread().getName());
                    return n % 2 == 0;
                })
                .map(n -> {
                    System.out.println("Асинхронне квадратування: " + n + " в потоці: " + Thread.currentThread().getName());
                    return n * n;
                })
                .observeOn(Schedulers.single())
                .subscribe(
                        result -> System.out.println("Асинхронний результат: " + result + " в потоці: " + Thread.currentThread().getName()),
                        error -> System.err.println("Помилка: " + error)
                );
    }

    // 3. Паралельна обробка з flatMap
    private static void demo3_ParallelProcessing(List<Integer> numbers) {
        System.out.println("\n3. ПАРАЛЕЛЬНА ОБРОБКА З FLATMAP:");

        Observable.fromIterable(numbers)
                .flatMap(n -> Observable.just(n)
                        .subscribeOn(Schedulers.computation())
                        .map(x -> {
                            String threadName = Thread.currentThread().getName();
                            System.out.println("Паралельна обробка " + x + " в потоці: " + threadName);
                            return x * x;
                        })
                )
                .toList()
                .subscribe(
                        results -> {
                            System.out.println("✅ Всі паралельні результати: " + results);
                            long sum = results.stream().mapToLong(Integer::longValue).sum();
                            System.out.println("Сума всіх квадратів: " + sum);
                        },
                        error -> System.err.println("Помилка: " + error)
                );
    }

    // 4. Комбінування потоків
    private static void demo4_CombiningStreams() {
        System.out.println("\n4. КОМБІНУВАННЯ ПОТОКІВ:");

        Observable<Integer> stream1 = Observable.just(1, 2, 3)
                .subscribeOn(Schedulers.computation())
                .map(n -> n * 10);

        Observable<Integer> stream2 = Observable.just(4, 5, 6)
                .subscribeOn(Schedulers.computation())
                .map(n -> n + 1);

        Observable.zip(stream1, stream2, (a, b) -> a + b)
                .subscribe(
                        result -> System.out.println("Zip результат: " + result),
                        error -> System.err.println("Помилка zip: " + error)
                );

        // Merge - об'єднує кілька Observable
        Observable<String> fastStream = Observable.interval(100, TimeUnit.MILLISECONDS)
                .take(3)
                .map(i -> "Fast-" + i);

        Observable<String> slowStream = Observable.interval(300, TimeUnit.MILLISECONDS)
                .take(2)
                .map(i -> "Slow-" + i);

        Observable.merge(fastStream, slowStream)
                .subscribe(
                        result -> System.out.println("Merge: " + result),
                        error -> System.err.println("Помилка merge: " + error),
                        () -> System.out.println("✅ Merge завершено!")
                );
    }

    // 5. Тест продуктивності (ВИПРАВЛЕНА ВЕРСІЯ)
    private static void demo5_PerformanceTest() {
        System.out.println("\n5. ТЕСТ ПРОДУКТИВНОСТІ НА 50K ЕЛЕМЕНТІВ:");

        List<Integer> largeList = generateLargeList();

        // Використовуємо AtomicLong для зберігання часу
        AtomicLong sequentialTime = new AtomicLong();
        AtomicLong parallelTime = new AtomicLong();

        long startSeq = System.currentTimeMillis();
        Observable.fromIterable(largeList)
                .filter(n -> n % 2 == 0)
                .map(n -> n * n)
                .reduce(0L, Long::sum)
                .subscribe(
                        result -> {
                            long endSeq = System.currentTimeMillis();
                            sequentialTime.set(endSeq - startSeq);
                            System.out.println("⏱️ Час послідовної обробки: " + sequentialTime.get() + " ms");
                            System.out.println("Сума квадратів (послідовно): " + result);
                        },
                        error -> System.err.println("Помилка послідовної обробки: " + error)
                );


        long startPar = System.currentTimeMillis();
        Observable.fromIterable(largeList)
                .flatMap(n -> Observable.just(n)
                        .subscribeOn(Schedulers.computation())
                        .filter(x -> x % 2 == 0)
                        .map(x -> x * x)
                )
                .reduce(0L, Long::sum)
                .subscribe(
                        result -> {
                            long endPar = System.currentTimeMillis();
                            parallelTime.set(endPar - startPar);
                            System.out.println("⏱️ Час паралельної обробки: " + parallelTime.get() + " ms");
                            System.out.println("Сума квадратів (паралельно): " + result);

                            // Порівняння продуктивності
                            if (sequentialTime.get() > 0 && parallelTime.get() > 0) {
                                long diff = sequentialTime.get() - parallelTime.get();
                                System.out.println("📊 Різниця в часі: " + Math.abs(diff) + " ms " +
                                        (diff > 0 ? "(паралельно швидше на " + diff + " ms)" :
                                                "(послідовно швидше на " + Math.abs(diff) + " ms)"));

                                double speedup = (double) sequentialTime.get() / parallelTime.get();
                                System.out.println("🚀 Приріст продуктивності: " + String.format("%.2f", speedup) + "x");
                            }
                        },
                        error -> System.err.println("Помилка паралельної обробки: " + error)
                );
    }


    private static List<Integer> generateLargeList() {
        return IntStream.rangeClosed(1, 50000)
                .boxed()
                .collect(Collectors.toList());
    }
}