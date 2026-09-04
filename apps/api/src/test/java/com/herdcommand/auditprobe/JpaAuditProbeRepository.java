package com.herdcommand.auditprobe;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaAuditProbeRepository extends JpaRepository<JpaAuditProbe, UUID> {}
