import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

public class WorkPipeline {

    /* ===================== 가중치 라운드로빈 구현 ===================== */
    public static final class WeightedRoundRobin<T> {
        private static final class Node<T> {
            final T item;              // 실제 노드(여기서는 Agent 이름)
            final int weight;          // 가중치 (처리 역량)
            int currentWeight = 0;     // 동적으로 변경되는 현재 가중치
            Node(T item, int weight) {
                if (weight <= 0) throw new IllegalArgumentException("weight must be > 0");
                this.item = item;
                this.weight = weight;
            }
        }

        private final List<Node<T>> nodes = new ArrayList<>(); // 등록된 노드들
        private int totalWeight = 0;                          // 전체 가중치 합
        private final ReentrantLock lock = new ReentrantLock(); // 스레드 안전성 확보용 Lock

        // 노드 추가
        public void add(T item, int weight) {
            lock.lock();
            try {
                nodes.add(new Node<>(item, weight));
                totalWeight += weight;
            } finally {
                lock.unlock();
            }
        }

        // 다음 노드 선택 (Smooth Weighted Round Robin 알고리즘)
        public T next() {
            lock.lock();
            try {
                if (nodes.isEmpty()) return null;
                if (nodes.size() == 1) return nodes.get(0).item;

                Node<T> best = null;
                for (Node<T> n : nodes) {
                    n.currentWeight += n.weight; // 각 노드의 현재 가중치 증가
                    if (best == null || n.currentWeight > best.currentWeight) {
                        best = n; // 가장 높은 currentWeight를 가진 노드 선택
                    }
                }
                best.currentWeight -= totalWeight; // 선택된 노드의 currentWeight 조정
                return best.item;
            } finally {
                lock.unlock();
            }
        }
    }
    /* ================================================================= */

    /** 작업 결과 구조체 */
    static final class Result {
        final int taskId;        // 작업 ID
        final String agentName;  // 처리한 Agent 이름
        Result(int taskId, String agentName) { this.taskId = taskId; this.agentName = agentName; }
        String toLine() { return String.format("%04d\t%s", taskId, agentName); } // works.txt에 저장할 라인 포맷
    }

    /** 실제 작업 실행(에이전트 처리 시뮬레이션) */
    static final class AgentWorker implements Callable<Result> {
        private final int taskId;
        private final String agentName;
        private final int agentWeight;

        AgentWorker(int taskId, String agentName, int agentWeight) {
            this.taskId = taskId;
            this.agentName = agentName;
            this.agentWeight = agentWeight;
        }

        @Override
        public Result call() throws Exception {
            // 간단한 처리 시간 시뮬레이션 (가중치가 클수록 빨리 처리되도록)
            int base = Math.max(5, 220 - agentWeight);
            Thread.sleep(ThreadLocalRandom.current().nextInt(3, base));
            return new Result(taskId, agentName);
        }
    }

    public static void main(String[] args) throws Exception {
        final int TOTAL_WORKS = 950; // 전체 작업 수

        // 1) 에이전트와 가중치 설정
        Map<String, Integer> weights = new LinkedHashMap<>();
        weights.put("Agent#1(cap=50)", 50);
        weights.put("Agent#2(cap=100)", 100);
        weights.put("Agent#3(cap=200)", 200);

        // 2) 가중치 라운드로빈 준비
        WeightedRoundRobin<String> wrr = new WeightedRoundRobin<>();
        for (Map.Entry<String, Integer> e : weights.entrySet()) {
            wrr.add(e.getKey(), e.getValue());
        }

        // 3) 스레드풀 생성 (멀티스레드로 작업 처리)
        int threads = Math.min(12, Math.max(3, Runtime.getRuntime().availableProcessors()));
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        // 4) 작업 분배 및 제출
        List<Future<Result>> futures = new ArrayList<>(TOTAL_WORKS);
        Map<String, Integer> assignedCounts = new LinkedHashMap<>();
        for (int i = 1; i <= TOTAL_WORKS; i++) {
            String agent = wrr.next(); // Weighted RR으로 다음 Agent 선택
            int w = weights.get(agent);
            futures.add(pool.submit(new AgentWorker(i, agent, w))); // AgentWorker에 작업 제출
            assignedCounts.merge(agent, 1, Integer::sum);
        }

        // 5) 완료 결과 모으기
        List<Result> results = new ArrayList<>(TOTAL_WORKS);
        for (Future<Result> f : futures) {
            results.add(f.get());
        }
        pool.shutdown(); // 모든 작업 제출 후 스레드풀 종료

        // 6) 결과를 taskId 순으로 정렬 후 works.txt에 저장
        results.sort(Comparator.comparingInt(r -> r.taskId));
        List<String> lines = new ArrayList<>(results.size());
        for (Result r : results) lines.add(r.toLine());
        Files.write(Path.of("works.txt"), lines, StandardCharsets.UTF_8);

        // 7) 콘솔에 분배 통계 출력
        System.out.println("=== Assigned counts by agent (Weighted RR) ===");
        assignedCounts.forEach((k, v) -> System.out.printf("%-18s : %d%n", k, v));
        System.out.println("saved -> works.txt");
    }
}