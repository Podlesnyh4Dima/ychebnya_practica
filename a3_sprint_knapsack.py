def knapsack(items, capacity):
    best = [0] * (capacity + 1)
    selected = [[False] * (capacity + 1) for _ in items]

    for i, (name, cost, value) in enumerate(items):
        if cost <= 0:
            raise ValueError("Трудоёмкость должна быть положительной")
        for hours in range(capacity, cost - 1, -1):
            candidate = best[hours - cost] + value
            if candidate > best[hours]:
                best[hours] = candidate
                selected[i][hours] = True

    result, hours = [], capacity
    for i in range(len(items) - 1, -1, -1):
        if selected[i][hours]:
            result.append(items[i][0])
            hours -= items[i][1]

    result.reverse()
    return best[capacity], result, capacity - hours


def greedy(items, capacity):
    ordered = sorted(items, key=lambda item: item[2] / item[1], reverse=True)
    result, value, used = [], 0, 0

    for name, cost, points in ordered:
        if cost <= 0:
            raise ValueError("Трудоёмкость должна быть положительной")
        if used + cost <= capacity:
            result.append(name)
            value += points
            used += cost

    return value, result, used


def main():
    backlog = [
        ("Туториал первой сессии", 16, 90),
        ("Ежедневные награды", 8, 55),
        ("Магазин (IAP)", 24, 100),
        ("Облачные сохранения", 20, 60),
        ("Сезонное событие", 32, 85),
        ("Рейтинг игроков", 14, 45),
        ("Оптимизация загрузки", 12, 50),
        ("Локализация (EN)", 10, 35),
        ("Система достижений", 18, 40),
        ("Исправление критических багов", 22, 95)
    ]
    capacity = 100

    for title, algorithm in (("Динамическое программирование", knapsack),
                             ("Жадный метод", greedy)):
        value, tasks, hours = algorithm(backlog, capacity)
        print(f"{title}: ценность {value}, занято {hours} из {capacity} ч")
        for task in tasks:
            print(" -", task)


if __name__ == "__main__":
    main()
