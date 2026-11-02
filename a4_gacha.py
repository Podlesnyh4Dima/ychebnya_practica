import bisect
import itertools
import random


def prepare(items):
    if not items or any(weight <= 0 for _, weight in items):
        raise ValueError("Вес каждой награды должен быть положительным")
    ids = [item for item, _ in items]
    sums = list(itertools.accumulate(weight for _, weight in items))
    return ids, sums


def choose(table, rng):
    ids, sums = table
    value = rng.random() * sums[-1]
    return ids[bisect.bisect_right(sums, value)]


def roll(rare, common, counter, rng, base=0.02,
         start=60, step=0.06, guarantee=90):
    if counter + 1 >= guarantee:
        chance = 1
    elif counter >= start:
        chance = min(1, base + (counter - start + 1) * step)
    else:
        chance = base

    if rng.random() < chance:
        return choose(rare, rng), True, 0
    return choose(common, rng), False, counter + 1


def main():
    rare = prepare([("Легенда A", 1), ("Легенда B", 3)])
    common = prepare([("Обычная 1", 50), ("Обычная 2", 30),
                      ("Обычная 3", 20)])
    rng = random.Random(2026)
    counter = 0

    for number in range(1, 11):
        item, is_rare, counter = roll(rare, common, counter, rng)
        print(number, item, "редкая:", is_rare, "счётчик:", counter)

    rng = random.Random(2026)
    counter = rare_count = longest_gap = 0
    attempts = 1_000_000

    for _ in range(attempts):
        _, is_rare, counter = roll(rare, common, counter, rng)
        rare_count += is_rare
        longest_gap = max(longest_gap, counter)

    print("Доля редких наград:", round(rare_count / attempts, 4))
    print("Максимальная серия без редкой награды:", longest_gap)


if __name__ == "__main__":
    main()
