#!/usr/bin/env python3
"""Generate BR provider CSVs from municipios_piloto (Story 3.4)."""
import os

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources")
MUNICIPIOS = os.path.join(ROOT, "br", "geography", "municipios_piloto.csv")
UFS = os.path.join(ROOT, "br", "geography", "ufs.csv")
OUT = os.path.join(ROOT, "br", "providers")

HEADER = (
    "provider_num,npi,name,address,city,state,zip,fips_county,lat,lon,phone,"
    "provider_type_code,category,emergency,upin,pin,region_code,bed_count,clia_lab_number"
)


def load_uf_names():
    names = {}
    with open(UFS, encoding="utf-8") as f:
        for line in f:
            if line.startswith("#") or line.startswith("sigla") or not line.strip():
                continue
            parts = line.strip().split(",")
            names[parts[0]] = parts[1]
    return names


def load_municipios():
    rows = []
    with open(MUNICIPIOS, encoding="utf-8") as f:
        for line in f:
            if line.startswith("#") or line.startswith("id,") or not line.strip():
                continue
            parts = line.strip().split(",")
            rows.append(parts)
    return rows


def main():
    uf_names = load_uf_names()
    municipios = load_municipios()
    os.makedirs(OUT, exist_ok=True)

    ubs_lines = [
        "# Fonte: Synthea-br MVP — UBS fictícias (100% sintético, NFR5). Story 3.4.",
        "# state = UF nome completo (contrato Story 3.2 / Person.STATE).",
        HEADER,
    ]
    hosp_lines = [
        "# Fonte: Synthea-br MVP — hospitais genéricos fictícios (100% sintético).",
        "# state = UF nome completo (contrato Story 3.2 / Person.STATE).",
        HEADER,
    ]

    for i, parts in enumerate(municipios):
        mid, nome, uf, lat, lon, cep_min, _cep_max = parts[:7]
        uf_nome = uf_names[uf]
        zip_digits = cep_min[:8]
        pnum_ubs = f"BR-UBS-{mid}"
        pnum_hosp = f"BR-HSP-{mid}"
        npi_ubs = f"9{int(mid) % 1000000000:09d}"
        npi_hosp = f"8{int(mid) % 1000000000:09d}"
        phone_ubs = f"113000{1000 + i:04d}"
        phone_hosp = f"113100{1000 + i:04d}"
        ubs_lines.append(
            f'"{pnum_ubs}","{npi_ubs}","UBS Modelo {nome}",'
            f'"Av. Saúde {i + 1}, 100","{nome}","{uf_nome}","{zip_digits}","",'
            f'{lat},{lon},"{phone_ubs}","00-04","21-01",false,,,,,'
        )
        hosp_lines.append(
            f'"{pnum_hosp}","{npi_hosp}","Hospital Geral Exemplo {nome}",'
            f'"Rua Hospital {i + 1}, 500","{nome}","{uf_nome}","{zip_digits}","",'
            f'{lat},{lon},"{phone_hosp}","00-09","01-02",true,,,"04",80,'
        )

    with open(os.path.join(OUT, "ubs.csv"), "w", encoding="utf-8", newline="") as f:
        f.write("\n".join(ubs_lines) + "\n")
    with open(os.path.join(OUT, "hospital_generico.csv"), "w", encoding="utf-8", newline="") as f:
        f.write("\n".join(hosp_lines) + "\n")
    print(f"Wrote {len(municipios)} UBS and {len(municipios)} hospital rows")


if __name__ == "__main__":
    main()
