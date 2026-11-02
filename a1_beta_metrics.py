import math
import random


def retention(installs, sessions, day, last_day):
    cohort = {user: install for user, install in installs}
    mature = {user for user, install in cohort.items() if install + day <= last_day}
    returned = {user for user, session_day in sessions
                if user in mature and session_day - cohort[user] == day}
    return len(returned) / len(mature) if mature else 0


def calculate_metrics(installs, sessions, purchases, spend, last_day):
    cohort = dict(installs)
    users = len(cohort)
    if not users:
        raise ValueError("Когорта не должна быть пустой")

    d1 = retention(installs, sessions, 1, last_day)
    d7 = retention(installs, sessions, 7, last_day)
    payers = {user for user, _, _ in purchases if user in cohort}
    revenue = sum(amount for user, _, amount in purchases if user in cohort)
    active_days = len({(user, day) for user, day in sessions if user in cohort})
    arpdau = revenue / active_days if active_days else 0
    cpi = spend / users
    alpha = max(0, math.log(d1 / d7) / math.log(7)) if d1 and d7 else 0
    ltv = arpdau * (1 + sum(d1 * day ** -alpha for day in range(1, 91)))
    roi = ltv / cpi if cpi else float("inf")

    return {
        "users": users,
        "d1": d1,
        "d7": d7,
        "conversion": len(payers) / users,
        "arpu": revenue / users,
        "arppu": revenue / len(payers) if payers else 0,
        "arpdau": arpdau,
        "cpi": cpi,
        "ltv": ltv,
        "roi": roi
    }


def main():
    rng = random.Random(42)
    users = 20000
    installs = [(user, 0) for user in range(users)]
    sessions, purchases = [], []

    for user in range(users):
        sessions.append((user, 0))
        for day, chance in ((1, 0.38), (3, 0.24), (7, 0.14), (14, 0.09)):
            if rng.random() < chance:
                sessions.append((user, day))
        if rng.random() < 0.035:
            purchases.append((user, rng.choice((0, 1, 3, 7)),
                              rng.choice((149, 299, 749))))

    metrics = calculate_metrics(installs, sessions, purchases, users * 45, 30)
    limits = {"d1": 0.35, "d7": 0.12, "roi": 1.0}
    failed = [name for name, limit in limits.items() if metrics[name] < limit]
    decision = "GO" if not failed else "RETURN_TO_PRODUCTION"

    for name, value in metrics.items():
        print(f"{name}: {value:.4f}" if isinstance(value, float)
              else f"{name}: {value}")
    print("Решение:", decision)
    print("Невыполненные критерии:", failed if failed else "нет")


if __name__ == "__main__":
    main()
