package org.teamforge.model;

/**
 * Ringkasan jumlah tugas tiap anggota. Hanya menunjukkan jumlah pekerjaan yang tercatat,
 * bukan penilaian kualitas anggota.
 */
public record MemberContribution(String name, String roleName, long assigned, long done) {

    public long open() {
        return assigned - done;
    }
}
