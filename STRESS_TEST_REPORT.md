# Stress Test Report: Knowledge Service Mock API

**Test Date:** 2026-06-08
**API:** `POST http://10.3.10.200:18080/api/knowledge`
**Tool:** Python ThreadPoolExecutor

---

## Summary

| Phase | Test Case | Total | Success | Fail | Avg(ms) | Median(ms) | P95(ms) | P99(ms) | Min(ms) | Max(ms) |
|-------|-----------|-------|---------|------|---------|------------|---------|---------|---------|---------|
| 100req x10con | RM1201 | 100 | 100 | 0 | 17.3 | 16.2 | 25.9 | 33.2 | 7.7 | 33.2 |
| 100req x10con | RM1202 | 100 | 100 | 0 | 22.9 | 22.5 | 38.7 | 55.3 | 9.3 | 55.3 |
| 500req x50con | RM1201 | 500 | 500 | 0 | 83.5 | 81.2 | 165.7 | 204.5 | 7.9 | 248.9 |
| 500req x50con | RM1202 | 500 | 500 | 0 | 80.6 | 74.7 | 167.4 | 195.1 | 5.6 | 286.5 |
| 1000req x100con | RM1201 | 1000 | 1000 | 0 | 163.7 | 134.4 | 496.3 | 755.6 | 8.8 | 1130.2 |
| 1000req x100con | RM1202 | 1000 | 1000 | 0 | 156.7 | 78.4 | 553.8 | 689.7 | 3.4 | 837.6 |

### RM1201 (100req x10con)

- **Total:** 100 | **Success:** 100 | **Fail:** 0
- **Response Codes:** {"0000": 100}

| Metric | Value (ms) |
|--------|-----------|
| Avg | 17.3 |
| Median | 16.2 |
| Min | 7.7 |
| Max | 33.2 |
| P95 | 25.9 |
| P99 | 33.2 |

### RM1202 (100req x10con)

- **Total:** 100 | **Success:** 100 | **Fail:** 0
- **Response Codes:** {"0000": 100}

| Metric | Value (ms) |
|--------|-----------|
| Avg | 22.9 |
| Median | 22.5 |
| Min | 9.3 |
| Max | 55.3 |
| P95 | 38.7 |
| P99 | 55.3 |

### RM1201 (500req x50con)

- **Total:** 500 | **Success:** 500 | **Fail:** 0
- **Response Codes:** {"0000": 500}

| Metric | Value (ms) |
|--------|-----------|
| Avg | 83.5 |
| Median | 81.2 |
| Min | 7.9 |
| Max | 248.9 |
| P95 | 165.7 |
| P99 | 204.5 |

### RM1202 (500req x50con)

- **Total:** 500 | **Success:** 500 | **Fail:** 0
- **Response Codes:** {"0000": 500}

| Metric | Value (ms) |
|--------|-----------|
| Avg | 80.6 |
| Median | 74.7 |
| Min | 5.6 |
| Max | 286.5 |
| P95 | 167.4 |
| P99 | 195.1 |

### RM1201 (1000req x100con)

- **Total:** 1000 | **Success:** 1000 | **Fail:** 0
- **Response Codes:** {"0000": 1000}

| Metric | Value (ms) |
|--------|-----------|
| Avg | 163.7 |
| Median | 134.4 |
| Min | 8.8 |
| Max | 1130.2 |
| P95 | 496.3 |
| P99 | 755.6 |

### RM1202 (1000req x100con)

- **Total:** 1000 | **Success:** 1000 | **Fail:** 0
- **Response Codes:** {"0000": 1000}

| Metric | Value (ms) |
|--------|-----------|
| Avg | 156.7 |
| Median | 78.4 |
| Min | 3.4 |
| Max | 837.6 |
| P95 | 553.8 |
| P99 | 689.7 |

---

## Conclusion

All tests ran on a single instance with default Tomcat thread pool (200 workers).
Key findings are summarized above. The mock service serves pre-built JSON files
directly with no backend computation, so latency is dominated by disk I/O and
network round-trip time.