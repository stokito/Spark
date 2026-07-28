from pathlib import Path

I18N_DIR = Path("core/src/main/resources/i18n")
SOURCE = I18N_DIR / "spark_i18n.properties"


def parse_properties(path: Path):
    entries = []
    values = {}
    comments_and_blank_by_next_key = {}
    pending = []

    for line in path.read_text(encoding="utf-8").splitlines(keepends=True):
        stripped = line.strip()

        if not stripped or stripped.startswith("#"):
            pending.append(line)
            continue

        key = None
        for sep in ("=", ":"):
            if sep in line:
                key = line.split(sep, 1)[0].strip()
                break

        if key is None:
            pending.append(line)
            continue

        comments_and_blank_by_next_key[key] = pending
        pending = []
        values[key] = line
        entries.append(key)

    return entries, values, comments_and_blank_by_next_key, pending


source_keys, _, source_comments, source_tail = parse_properties(SOURCE)

for target in sorted(I18N_DIR.glob("spark_i18n_*.properties")):
    _, target_values, _, target_tail = parse_properties(target)

    output = []
    emitted = set()

    for key in source_keys:
        output.extend(source_comments.get(key, []))

        if key in target_values:
            output.append(target_values[key])
            emitted.add(key)

    extras = [key for key in target_values if key not in emitted]
    if extras:
        if output and output[-1].strip():
            output.append("\n")
        output.append("## Locale-specific entries not present in spark_i18n.properties\n")
        for key in extras:
            output.append(target_values[key])

    output.extend(source_tail)
    target.write_text("".join(output), encoding="utf-8")
    print(f"Reordered {target}")
