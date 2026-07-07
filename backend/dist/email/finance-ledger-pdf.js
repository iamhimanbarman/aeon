import { createRequire } from "node:module";
const require = createRequire(import.meta.url);
const PDFDocument = require("pdfkit");
const page = {
    width: 595.28,
    height: 841.89,
    margin: 36
};
const colors = {
    ink: "#F8FAFC",
    muted: "#AAB1C2",
    faint: "#6E7688",
    background: "#07080C",
    panel: "#11141E",
    panelSoft: "#161A27",
    line: "#2B3142",
    gold: "#F5C542",
    goldSoft: "#2E2715",
    brand: "#8B6CFF",
    success: "#22C55E"
};
export async function buildFinanceLedgerStatementPdf(input) {
    const doc = new PDFDocument({
        size: "A4",
        margin: 0,
        bufferPages: true,
        info: {
            Title: "Aeon Ledger Statement",
            Author: "Aeon",
            Subject: "Open ledger records"
        }
    });
    const chunks = [];
    const done = new Promise((resolve, reject) => {
        doc.on("data", (chunk) => {
            chunks.push(Buffer.isBuffer(chunk) ? chunk : Buffer.from(chunk));
        });
        doc.on("end", () => resolve(Buffer.concat(chunks)));
        doc.on("error", reject);
    });
    drawBackground(doc);
    drawHeader(doc, input);
    drawSummary(doc, input);
    drawRecordsTable(doc, input);
    drawFooter(doc);
    doc.end();
    return done;
}
function drawBackground(doc) {
    doc.rect(0, 0, page.width, page.height).fill(colors.background);
    doc.save();
    doc.fillColor("#111525").opacity(0.85);
    doc.roundedRect(332, -52, 250, 250, 48).fill();
    doc.fillColor("#231C11").opacity(0.72);
    doc.roundedRect(-70, 620, 220, 220, 54).fill();
    doc.restore();
}
function drawHeader(doc, input) {
    doc.fillColor(colors.gold);
    doc.roundedRect(page.margin, 34, 206, 38, 19).fill(colors.goldSoft).stroke(colors.gold);
    doc.font("Helvetica-Bold")
        .fontSize(10)
        .fillColor(colors.gold)
        .text("AEON FINANCE STATEMENT", page.margin + 18, 47, {
        width: 174,
        characterSpacing: 1.7
    });
    doc.font("Helvetica-Bold")
        .fontSize(34)
        .fillColor(colors.ink)
        .text("Open ledger account", page.margin, 104, {
        width: 350,
        lineGap: 4
    });
    doc.font("Helvetica")
        .fontSize(12)
        .fillColor(colors.muted)
        .text(`Prepared for ${input.recipientName}`, page.margin, 190, {
        width: 280
    });
    doc.font("Helvetica")
        .fontSize(11)
        .fillColor(colors.faint)
        .text(`Shared by ${input.ownerName}${input.ownerEmail ? ` | ${input.ownerEmail}` : ""}`, page.margin, 210, {
        width: 380
    });
    drawAeonMark(doc, 470, 54);
}
function drawAeonMark(doc, x, y) {
    doc.save();
    doc.circle(x + 24, y + 24, 24).fill("#131722").stroke(colors.line);
    doc.font("Helvetica-Bold")
        .fontSize(24)
        .fillColor(colors.gold)
        .text("A", x + 15, y + 11, { width: 26, align: "center" });
    doc.font("Helvetica-Bold")
        .fontSize(8)
        .fillColor(colors.muted)
        .text("AEON", x - 2, y + 56, { width: 52, align: "center", characterSpacing: 1.4 });
    doc.restore();
}
function drawSummary(doc, input) {
    const totals = summarize(input.records);
    const newRecord = input.records.find((record) => record.id === input.newRecordId) ?? input.records.at(-1);
    const totalLabel = totals.netRecipientOwes > 0
        ? "You owe"
        : totals.netRecipientOwes < 0
            ? "You receive"
            : "Open net";
    const totalAmount = totals.netRecipientOwes === 0
        ? formatCurrency(0, totals.currency)
        : formatCurrency(Math.abs(totals.netRecipientOwes), totals.currency);
    drawMetricCard(doc, {
        x: page.margin,
        y: 250,
        width: 250,
        title: "New record",
        value: newRecord ? formatCurrency(Number(newRecord.amount), newRecord.currency) : formatCurrency(0, totals.currency),
        caption: newRecord ? truncate(newRecord.purpose, 70) : "No new record found",
        accent: colors.gold
    });
    drawMetricCard(doc, {
        x: page.margin + 272,
        y: 250,
        width: 250,
        title: "Open total",
        value: totalAmount,
        caption: `${totalLabel} | ${input.records.length} open record${input.records.length === 1 ? "" : "s"}`,
        accent: totals.netRecipientOwes >= 0 ? colors.brand : colors.success
    });
}
function drawMetricCard(doc, metric) {
    doc.save();
    doc.roundedRect(metric.x, metric.y, metric.width, 96, 22).fill(colors.panel).stroke(colors.line);
    doc.roundedRect(metric.x + 14, metric.y + 14, 74, 20, 10).fill(metric.accent === colors.gold ? colors.goldSoft : "#1A1830").stroke(metric.accent);
    doc.font("Helvetica-Bold")
        .fontSize(8)
        .fillColor(metric.accent)
        .text(metric.title.toUpperCase(), metric.x + 24, metric.y + 20, {
        width: 120,
        characterSpacing: 1.3
    });
    doc.font("Helvetica-Bold")
        .fontSize(23)
        .fillColor(colors.ink)
        .text(metric.value, metric.x + 16, metric.y + 44, {
        width: metric.width - 32,
        continued: false
    });
    doc.font("Helvetica")
        .fontSize(10)
        .fillColor(colors.muted)
        .text(metric.caption, metric.x + 16, metric.y + 72, {
        width: metric.width - 32,
        ellipsis: true
    });
    doc.restore();
}
function drawRecordsTable(doc, input) {
    let y = 386;
    const x = page.margin;
    const tableWidth = page.width - page.margin * 2;
    const columns = {
        date: 76,
        type: 72,
        amount: 90,
        status: 58
    };
    const purposeWidth = tableWidth - columns.date - columns.type - columns.amount - columns.status - 30;
    drawTableHeader(doc, x, y, tableWidth);
    y += 34;
    input.records.forEach((record, index) => {
        if (y > page.height - 118) {
            doc.addPage({ size: "A4", margin: 0 });
            drawBackground(doc);
            drawFooter(doc);
            y = 56;
            drawTableHeader(doc, x, y, tableWidth);
            y += 34;
        }
        const isNew = record.id === input.newRecordId;
        const rowHeight = 46;
        doc.save();
        doc.roundedRect(x, y, tableWidth, rowHeight, 15)
            .fill(isNew ? "#1E1B2E" : index % 2 === 0 ? colors.panel : colors.panelSoft)
            .stroke(isNew ? colors.gold : colors.line);
        doc.font("Helvetica")
            .fontSize(9)
            .fillColor(colors.muted)
            .text(formatShortDate(record.occurredAt), x + 12, y + 15, { width: columns.date });
        doc.font("Helvetica-Bold")
            .fontSize(9)
            .fillColor(record.direction === "owed_to_me" ? colors.gold : colors.brand)
            .text(record.direction === "owed_to_me" ? "Lend" : "Borrow", x + 12 + columns.date, y + 15, {
            width: columns.type
        });
        doc.font("Helvetica")
            .fontSize(9)
            .fillColor(colors.ink)
            .text(truncate(record.purpose, 42), x + 12 + columns.date + columns.type, y + 10, {
            width: purposeWidth,
            height: 26,
            ellipsis: true
        });
        doc.font("Helvetica-Bold")
            .fontSize(9)
            .fillColor(colors.ink)
            .text(formatCurrency(Number(record.amount), record.currency), x + tableWidth - columns.amount - columns.status - 16, y + 15, {
            width: columns.amount,
            align: "right"
        });
        doc.font("Helvetica-Bold")
            .fontSize(8)
            .fillColor(isNew ? colors.gold : colors.success)
            .text(isNew ? "NEW" : "OPEN", x + tableWidth - columns.status - 4, y + 16, {
            width: columns.status,
            align: "center"
        });
        doc.restore();
        y += rowHeight + 8;
    });
    const totals = summarize(input.records);
    const netLabel = totals.netRecipientOwes > 0
        ? `Net: ${input.recipientName} owes ${input.ownerName}`
        : totals.netRecipientOwes < 0
            ? `Net: ${input.ownerName} owes ${input.recipientName}`
            : "Net: open account is balanced";
    y += 8;
    doc.roundedRect(x, y, tableWidth, 54, 18).fill("#171B28").stroke(colors.gold);
    doc.font("Helvetica-Bold")
        .fontSize(12)
        .fillColor(colors.ink)
        .text(netLabel, x + 16, y + 14, { width: tableWidth - 180 });
    doc.font("Helvetica-Bold")
        .fontSize(18)
        .fillColor(colors.gold)
        .text(formatCurrency(Math.abs(totals.netRecipientOwes), totals.currency), x + tableWidth - 156, y + 14, {
        width: 136,
        align: "right"
    });
}
function drawTableHeader(doc, x, y, width) {
    doc.font("Helvetica-Bold")
        .fontSize(12)
        .fillColor(colors.ink)
        .text("Open records", x, y - 26, { width });
    doc.roundedRect(x, y, width, 28, 14).fill("#0F121B").stroke(colors.line);
    doc.font("Helvetica-Bold")
        .fontSize(8)
        .fillColor(colors.faint)
        .text("DATE", x + 12, y + 10, { width: 76, characterSpacing: 1.1 })
        .text("TYPE", x + 88, y + 10, { width: 72, characterSpacing: 1.1 })
        .text("PURPOSE", x + 160, y + 10, { width: 180, characterSpacing: 1.1 })
        .text("AMOUNT", x + width - 142, y + 10, { width: 82, align: "right", characterSpacing: 1.1 })
        .text("STATE", x + width - 52, y + 10, { width: 44, align: "center", characterSpacing: 1.1 });
}
function drawFooter(doc) {
    doc.font("Helvetica")
        .fontSize(9)
        .fillColor(colors.faint)
        .text("Generated by Aeon Finance. Confirm balances directly before settlement.", page.margin, page.height - 44, {
        width: page.width - page.margin * 2,
        align: "center"
    });
}
function summarize(records) {
    const currency = records[0]?.currency ?? "INR";
    const owedToOwner = records
        .filter((record) => record.direction === "owed_to_me")
        .reduce((total, record) => total + Number(record.amount), 0);
    const ownerOwes = records
        .filter((record) => record.direction === "i_owe")
        .reduce((total, record) => total + Number(record.amount), 0);
    return {
        currency,
        owedToOwner,
        ownerOwes,
        netRecipientOwes: owedToOwner - ownerOwes
    };
}
function formatCurrency(amount, currency) {
    try {
        return new Intl.NumberFormat("en-IN", {
            style: "currency",
            currency,
            maximumFractionDigits: 2
        }).format(amount);
    }
    catch {
        return `${amount.toFixed(2)} ${currency}`;
    }
}
function formatShortDate(value) {
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
        return value.slice(0, 11);
    }
    return new Intl.DateTimeFormat("en-IN", {
        day: "2-digit",
        month: "short"
    }).format(date);
}
function truncate(value, maxLength) {
    return value.length <= maxLength ? value : `${value.slice(0, Math.max(0, maxLength - 1))}...`;
}
