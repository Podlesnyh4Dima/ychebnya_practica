import java.util.*;

public final class GameDevAlgorithms {
    private GameDevAlgorithms() {}

    public interface Algorithm<I, O> {
        O execute(I input);
    }

    public abstract static class UseCase<I, O> implements Algorithm<I, O> {
        @Override
        public final O execute(I input) {
            Objects.requireNonNull(input, "Входные данные не должны быть null");
            return calculate(input);
        }

        protected abstract O calculate(I input);
    }

    public record Install(int playerId, int day) {}
    public record Session(int playerId, int day) {}
    public record Purchase(int playerId, int day, double amount) {}
    public record BetaInput(List<Install> installs, List<Session> sessions,
                            List<Purchase> purchases, double spend, int lastDay,
                            double minD1, double minD7, double minRoi) {}
    public record BetaResult(double d1, double d7, double conversion,
                             double arpu, double arppu, double cpi,
                             double arpdau, double ltv, double roi,
                             boolean go, List<String> failedCriteria) {}

    public static final class BetaMetricsAlgorithm
            extends UseCase<BetaInput, BetaResult> {
        @Override
        protected BetaResult calculate(BetaInput in) {
            Map<Integer, Integer> cohort = new HashMap<>();
            for (Install x : in.installs()) cohort.put(x.playerId(), x.day());
            if (cohort.isEmpty()) throw new IllegalArgumentException("Пустая когорта");

            double d1 = retention(in, cohort, 1);
            double d7 = retention(in, cohort, 7);
            Set<Integer> payers = new HashSet<>();
            double revenue = 0;
            for (Purchase p : in.purchases()) {
                if (cohort.containsKey(p.playerId())) {
                    payers.add(p.playerId());
                    revenue += p.amount();
                }
            }
            Set<String> activeDays = new HashSet<>();
            for (Session s : in.sessions()) {
                if (cohort.containsKey(s.playerId()))
                    activeDays.add(s.playerId() + ":" + s.day());
            }

            int users = cohort.size();
            double arpu = revenue / users;
            double arppu = payers.isEmpty() ? 0 : revenue / payers.size();
            double cpi = in.spend() / users;
            double arpdau = activeDays.isEmpty() ? 0 : revenue / activeDays.size();
            double alpha = d1 > 0 && d7 > 0
                    ? Math.max(0, Math.log(d1 / d7) / Math.log(7)) : 0;
            double ltv = arpdau;
            for (int day = 1; day <= 90; day++)
                ltv += arpdau * d1 * Math.pow(day, -alpha);
            double roi = cpi == 0 ? Double.POSITIVE_INFINITY : ltv / cpi;

            List<String> failed = new ArrayList<>();
            if (d1 < in.minD1()) failed.add("D1 ниже порога");
            if (d7 < in.minD7()) failed.add("D7 ниже порога");
            if (roi < in.minRoi()) failed.add("ROI ниже порога");
            return new BetaResult(d1, d7, (double) payers.size() / users,
                    arpu, arppu, cpi, arpdau, ltv, roi, failed.isEmpty(),
                    List.copyOf(failed));
        }

        private double retention(BetaInput in, Map<Integer, Integer> cohort, int day) {
            Set<Integer> mature = new HashSet<>();
            for (var entry : cohort.entrySet())
                if (entry.getValue() + day <= in.lastDay()) mature.add(entry.getKey());
            if (mature.isEmpty()) return 0;
            Set<Integer> returned = new HashSet<>();
            for (Session s : in.sessions()) {
                Integer installDay = cohort.get(s.playerId());
                if (mature.contains(s.playerId()) && s.day() - installDay == day)
                    returned.add(s.playerId());
            }
            return (double) returned.size() / mature.size();
        }
    }

    public record ProjectTask(int duration, List<String> predecessors) {}
    public record ScheduleResult(List<String> order, Map<String, Integer> earlyStart,
                                 Map<String, Integer> earlyFinish,
                                 Map<String, Integer> lateStart,
                                 Map<String, Integer> lateFinish,
                                 Map<String, Integer> slack,
                                 List<String> criticalPath, int duration) {}

    public static final class CriticalPathAlgorithm
            extends UseCase<Map<String, ProjectTask>, ScheduleResult> {
        @Override
        protected ScheduleResult calculate(Map<String, ProjectTask> tasks) {
            Map<String, List<String>> next = new HashMap<>();
            Map<String, Integer> degree = new HashMap<>();
            tasks.keySet().forEach(id -> { next.put(id, new ArrayList<>()); degree.put(id, 0); });
            for (var entry : tasks.entrySet()) {
                if (entry.getValue().duration() <= 0)
                    throw new IllegalArgumentException("Длительность должна быть положительной");
                for (String p : entry.getValue().predecessors()) {
                    if (!tasks.containsKey(p)) throw new IllegalArgumentException("Неизвестная задача: " + p);
                    next.get(p).add(entry.getKey());
                    degree.merge(entry.getKey(), 1, Integer::sum);
                }
            }
            Queue<String> ready = new ArrayDeque<>();
            degree.forEach((id, d) -> { if (d == 0) ready.add(id); });
            List<String> order = new ArrayList<>();
            while (!ready.isEmpty()) {
                String id = ready.remove(); order.add(id);
                for (String child : next.get(id))
                    if (degree.merge(child, -1, Integer::sum) == 0) ready.add(child);
            }
            if (order.size() != tasks.size()) throw new IllegalArgumentException("Цикл зависимостей");

            Map<String, Integer> es = new HashMap<>(), ef = new HashMap<>();
            Map<String, String> parentOnPath = new HashMap<>();
            for (String id : order) {
                int best = 0; String bestParent = null;
                for (String p : tasks.get(id).predecessors())
                    if (ef.get(p) > best) { best = ef.get(p); bestParent = p; }
                es.put(id, best); ef.put(id, best + tasks.get(id).duration());
                if (bestParent != null) parentOnPath.put(id, bestParent);
            }
            String end = null; int duration = 0;
            for (String id : order) if (ef.get(id) > duration) { end = id; duration = ef.get(id); }
            Map<String, Integer> lf = new HashMap<>(), ls = new HashMap<>(), slack = new HashMap<>();
            for (int i = order.size() - 1; i >= 0; i--) {
                String id = order.get(i);
                int latestFinish = duration;
                for (String child : next.get(id)) latestFinish = Math.min(latestFinish, ls.get(child));
                lf.put(id, latestFinish);
                ls.put(id, latestFinish - tasks.get(id).duration());
                slack.put(id, ls.get(id) - es.get(id));
            }
            List<String> path = new ArrayList<>();
            while (end != null) { path.add(end); end = parentOnPath.get(end); }
            Collections.reverse(path);
            return new ScheduleResult(List.copyOf(order), Map.copyOf(es), Map.copyOf(ef),
                    Map.copyOf(ls), Map.copyOf(lf), Map.copyOf(slack), List.copyOf(path), duration);
        }
    }

    public record BacklogItem(String name, int hours, int value) {}
    public record SprintResult(int value, List<String> tasks, int usedHours) {}

    public static final class SprintSelectionAlgorithm
            extends UseCase<List<BacklogItem>, SprintResult> {
        private final int capacity;
        public SprintSelectionAlgorithm(int capacity) {
            if (capacity < 0) throw new IllegalArgumentException("Отрицательная ёмкость");
            this.capacity = capacity;
        }
        @Override
        protected SprintResult calculate(List<BacklogItem> items) {
            int n = items.size();
            int[][] dp = new int[n + 1][capacity + 1];
            for (int i = 1; i <= n; i++) {
                BacklogItem x = items.get(i - 1);
                if (x.hours() <= 0) throw new IllegalArgumentException("Трудоёмкость должна быть положительной");
                for (int h = 0; h <= capacity; h++) {
                    dp[i][h] = dp[i - 1][h];
                    if (x.hours() <= h)
                        dp[i][h] = Math.max(dp[i][h], dp[i - 1][h - x.hours()] + x.value());
                }
            }
            List<String> selected = new ArrayList<>(); int h = capacity, used = 0;
            for (int i = n; i > 0; i--) {
                BacklogItem x = items.get(i - 1);
                if (x.hours() <= h && dp[i][h] > dp[i - 1][h]
                        && dp[i][h] == dp[i - 1][h - x.hours()] + x.value()) {
                    selected.add(x.name()); h -= x.hours(); used += x.hours();
                }
            }
            Collections.reverse(selected);
            return new SprintResult(dp[n][capacity], List.copyOf(selected), used);
        }
    }

    public record WeightedReward(String name, double weight, boolean rare) {}
    public record RewardTable(List<WeightedReward> items, List<Double> cumulative,
                              double totalWeight) {}
    public static RewardTable prepareRewards(List<WeightedReward> items) {
        if (items.isEmpty()) throw new IllegalArgumentException("Таблица наград пуста");
        List<Double> cumulative = new ArrayList<>(); double total = 0;
        for (WeightedReward item : items) {
            if (item.weight() <= 0) throw new IllegalArgumentException("Вес должен быть положительным");
            total += item.weight(); cumulative.add(total);
        }
        return new RewardTable(List.copyOf(items), List.copyOf(cumulative), total);
    }
    public record DropInput(RewardTable rare, RewardTable common, int failureCount,
                            int start, int guarantee, double baseChance,
                            double stepChance, double chanceRoll, double itemRoll) {}
    public record DropResult(String reward, boolean rare, int nextFailureCount) {}

    public static final class GuaranteedDropAlgorithm
            extends UseCase<DropInput, DropResult> {
        @Override
        protected DropResult calculate(DropInput in) {
            double chance = in.failureCount() + 1 >= in.guarantee() ? 1
                    : in.failureCount() >= in.start()
                    ? Math.min(1, in.baseChance() + (in.failureCount() - in.start() + 1) * in.stepChance())
                    : in.baseChance();
            boolean isRare = in.chanceRoll() < chance;
            WeightedReward reward = choose(isRare ? in.rare() : in.common(), in.itemRoll());
            return new DropResult(reward.name(), isRare, isRare ? 0 : in.failureCount() + 1);
        }
        private WeightedReward choose(RewardTable table, double randomValue) {
            double target = randomValue * table.totalWeight();
            int low = 0, high = table.cumulative().size();
            while (low < high) {
                int mid = (low + high) / 2;
                if (table.cumulative().get(mid) <= target) low = mid + 1;
                else high = mid;
            }
            int index = low;
            return table.items().get(Math.min(index, table.items().size() - 1));
        }
    }

    public record PlayerScore(int playerId, int score) {}
    public record LeaderboardInput(List<PlayerScore> scores, int k) {}

    public static final class TopKAlgorithm
            extends UseCase<LeaderboardInput, List<PlayerScore>> {
        @Override
        protected List<PlayerScore> calculate(LeaderboardInput in) {
            if (in.k() <= 0) return List.of();
            Comparator<PlayerScore> weakestFirst = Comparator.comparingInt(PlayerScore::score)
                    .thenComparing(Comparator.comparingInt(PlayerScore::playerId).reversed());
            PriorityQueue<PlayerScore> heap = new PriorityQueue<>(weakestFirst);
            for (PlayerScore x : in.scores()) {
                if (heap.size() < in.k()) heap.add(x);
                else if (weakestFirst.compare(x, heap.peek()) > 0) { heap.remove(); heap.add(x); }
            }
            return heap.stream().sorted(Comparator.comparingInt(PlayerScore::score).reversed()
                    .thenComparingInt(PlayerScore::playerId)).toList();
        }
    }
}
