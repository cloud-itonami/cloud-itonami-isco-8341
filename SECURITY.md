# Security Policy

This project handles mobile farm and forestry plant operators scheduling and
logistics coordination workflows, including physical-safety-adjacent data
(equipment defects, terrain hazards, operator fatigue). Treat vulnerabilities
as potentially high impact even when the demo data is synthetic.

## Do Not Disclose Publicly

Report privately before opening public issues for:

- credential exposure
- real operator or equipment data exposure
- authorization bypass
- Farm/Forestry Ops Governor bypass
- any path that lets a proposal finalize an equipment-operation/movement decision or override an operator's on-site safety judgment
- audit-ledger tampering
- over-disclosure in reports or exports
- unsafe robot action dispatch

## Reporting

Use GitHub private vulnerability reporting when available for the repository.
If that is unavailable, contact the repository maintainers through the
cloud-itonami organization before publishing details.

Include:

- affected commit or version
- reproduction steps
- expected and actual behavior
- impact on operator/equipment data, policy enforcement or audit logging
- suggested fix, if known

## Production Guidance

- Store secrets outside Git.
- Keep real operator/equipment data outside this repository.
- Run policy tests before deployment.
- Export and review audit logs regularly.
- Use least privilege for operators and service accounts.
