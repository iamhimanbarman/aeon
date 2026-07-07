import { existsSync } from "node:fs";
import { createRequire } from "node:module";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
const require = createRequire(import.meta.url);
const PDFDocument = require("pdfkit");
const requireDir = dirname(fileURLToPath(import.meta.url));
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
    tableHeader: "#0D1018",
    line: "#2B3142",
    gold: "#F5C542",
    goldSoft: "#2E2715",
    brand: "#8B6CFF",
    success: "#22C55E",
    successSoft: "#102218",
    danger: "#F87171",
    dangerSoft: "#241216"
};
const tableColumns = [
    { key: "date", label: "DATE", width: 72 },
    { key: "type", label: "TYPE", width: 76 },
    { key: "purpose", label: "PURPOSE", width: 202 },
    { key: "amount", label: "AMOUNT", width: 98, align: "right" },
    { key: "status", label: "STATE", width: 56, align: "center" }
];
export async function buildFinanceLedgerStatementPdf(input) {
    const doc = new PDFDocument({
        size: "A4",
        margin: 0,
        bufferPages: true,
        info: {
            Title: "Aeon Ledger Statement",
            Author: "Aeon",
            Subject: input.statementTitle ?? "Ledger records"
        }
    });
    const chunks = [];
    const resources = resolvePdfResources(doc);
    const done = new Promise((resolve, reject) => {
        doc.on("data", (chunk) => {
            chunks.push(Buffer.isBuffer(chunk) ? chunk : Buffer.from(chunk));
        });
        doc.on("end", () => resolve(Buffer.concat(chunks)));
        doc.on("error", reject);
    });
    drawBackground(doc);
    drawHeader(doc, input, resources);
    drawSummary(doc, input, resources);
    drawLetter(doc, input, resources);
    drawRecordsTable(doc, input, resources);
    drawFooter(doc, resources);
    doc.end();
    return done;
}
function drawBackground(doc) {
    doc.rect(0, 0, page.width, page.height).fill(colors.background);
    doc.save();
    doc.fillColor("#111525").opacity(0.88);
    doc.roundedRect(332, -52, 250, 250, 48).fill();
    doc.fillColor("#231C11").opacity(0.76);
    doc.roundedRect(-70, 620, 220, 220, 54).fill();
    doc.restore();
}
function drawHeader(doc, input, resources) {
    doc.roundedRect(page.margin, 34, 206, 38, 19).fillAndStroke(colors.goldSoft, colors.gold);
    doc.font(resources.boldFont)
        .fontSize(10)
        .fillColor(colors.gold)
        .text("AEON FINANCE STATEMENT", page.margin + 18, 47, {
        width: 174,
        characterSpacing: 1.7
    });
    doc.font(resources.boldFont)
        .fontSize(34)
        .fillColor(colors.ink)
        .text(input.statementTitle ?? "Open ledger account", page.margin, 104, {
        width: 330,
        lineGap: 4
    });
    doc.font(resources.regularFont)
        .fontSize(12)
        .fillColor(colors.muted)
        .text(`Prepared for ${input.recipientName}`, page.margin, 190, {
        width: 280
    });
    doc.font(resources.regularFont)
        .fontSize(11)
        .fillColor(colors.faint)
        .text(`Shared by ${input.ownerName}${input.ownerEmail ? ` | ${input.ownerEmail}` : ""}`, page.margin, 210, {
        width: 380
    });
    drawAeonMark(doc, resources, 392, 34);
}
function drawAeonMark(doc, resources, x, y) {
    doc.roundedRect(x, y, 166, 168, 38).fillAndStroke("#0E1220", colors.line);
    if (resources.logoPath) {
        try {
            doc.image(resources.logoPath, x + 44, y + 24, {
                fit: [78, 78],
                align: "center",
                valign: "center"
            });
        }
        catch {
            drawFallbackAeonMark(doc, resources, x, y);
            return;
        }
    }
    else {
        drawFallbackAeonMark(doc, resources, x, y);
        return;
    }
    doc.font(resources.boldFont)
        .fontSize(9)
        .fillColor(colors.muted)
        .text("AEON", x + 32, y + 114, {
        width: 102,
        align: "center",
        characterSpacing: 1.7
    });
}
function drawFallbackAeonMark(doc, resources, x, y) {
    doc.circle(x + 83, y + 58, 28).fillAndStroke("#131722", colors.line);
    doc.font(resources.boldFont)
        .fontSize(26)
        .fillColor(colors.gold)
        .text("A", x + 68, y + 45, { width: 30, align: "center" });
    doc.font(resources.boldFont)
        .fontSize(9)
        .fillColor(colors.muted)
        .text("AEON", x + 32, y + 114, {
        width: 102,
        align: "center",
        characterSpacing: 1.7
    });
}
function drawSummary(doc, input, resources) {
    const totals = summarize(input.records);
    const newRecord = input.newRecordId
        ? input.records.find((record) => record.id === input.newRecordId)
        : undefined;
    const primaryRecord = newRecord ?? input.records.at(-1);
    const primaryView = primaryRecord ? toRecipientRecordView(primaryRecord) : undefined;
    const netTone = toneForSignedAmount(totals.recipientNetAmount);
    const totalLabel = totals.recipientNetAmount > 0
        ? "You receive"
        : totals.recipientNetAmount < 0
            ? "You owe"
            : "Net settled";
    drawMetricCard(doc, resources, {
        x: page.margin,
        y: 250,
        width: 250,
        title: input.newRecordId
            ? `New ${primaryView?.typeLabel ?? "record"}`
            : "Selected net",
        value: primaryRecord
            ? primaryView?.amountLabel ?? formatCurrency(Number(primaryRecord.amount), primaryRecord.currency)
            : formatCurrency(0, totals.currency),
        caption: input.newRecordId
            ? primaryRecord
                ? truncate(primaryRecord.purpose, 70)
                : "No new record found"
            : `${input.records.length} selected record${input.records.length === 1 ? "" : "s"}`,
        accent: primaryView?.accent ?? colors.gold,
        valueColor: primaryView?.accent ?? colors.ink
    });
    drawMetricCard(doc, resources, {
        x: page.margin + 272,
        y: 250,
        width: 250,
        title: input.newRecordId ? "Open total" : "Selected total",
        value: formatSignedCurrency(totals.recipientNetAmount, totals.currency),
        caption: `${totalLabel} | ${input.records.length} ${input.newRecordId ? "open" : "selected"} record${input.records.length === 1 ? "" : "s"}`,
        accent: netTone.accent,
        valueColor: netTone.accent
    });
}
function drawLetter(doc, input, resources) {
    if (!input.letterMessage?.trim()) {
        return;
    }
    const x = page.margin;
    const y = 364;
    const width = page.width - page.margin * 2;
    doc.roundedRect(x, y, width, 54, 18).fillAndStroke("#10131D", colors.line);
    doc.font(resources.boldFont)
        .fontSize(8)
        .fillColor(colors.gold)
        .text(`MESSAGE FROM ${input.ownerName.toUpperCase()}`, x + 16, y + 12, {
        width: width - 32,
        characterSpacing: 1.2
    });
    doc.font(resources.regularFont)
        .fontSize(10)
        .fillColor(colors.muted)
        .text(truncate(input.letterMessage, 190), x + 16, y + 28, {
        width: width - 32,
        height: 18,
        ellipsis: true
    });
}
function drawMetricCard(doc, resources, metric) {
    doc.roundedRect(metric.x, metric.y, metric.width, 96, 22).fillAndStroke(colors.panel, colors.line);
    doc.roundedRect(metric.x + 14, metric.y + 14, 104, 20, 10).fillAndStroke("#151929", metric.accent);
    doc.font(resources.boldFont)
        .fontSize(8)
        .fillColor(metric.accent)
        .text(metric.title.toUpperCase(), metric.x + 22, metric.y + 20, {
        width: 156,
        characterSpacing: 1.2
    });
    doc.font(resources.boldFont)
        .fontSize(23)
        .fillColor(metric.valueColor)
        .text(metric.value, metric.x + 16, metric.y + 44, {
        width: metric.width - 32
    });
    doc.font(resources.regularFont)
        .fontSize(10)
        .fillColor(colors.muted)
        .text(metric.caption, metric.x + 16, metric.y + 72, {
        width: metric.width - 32,
        ellipsis: true
    });
}
function drawRecordsTable(doc, input, resources) {
    const x = page.margin;
    const width = page.width - page.margin * 2;
    const title = input.tableTitle ?? "Open records";
    const rowHeight = 34;
    const headerHeight = 30;
    let y = input.letterMessage?.trim() ? 456 : 386;
    let recordIndex = 0;
    let isFirstPage = true;
    while (recordIndex < input.records.length || (input.records.length === 0 && isFirstPage)) {
        if (!isFirstPage) {
            doc.addPage({ size: "A4", margin: 0 });
            drawBackground(doc);
            drawFooter(doc, resources);
            y = 68;
        }
        const availableHeight = page.height - y - 122;
        const rowsPerPage = Math.max(1, Math.floor((availableHeight - headerHeight) / rowHeight));
        const pageRecords = input.records.slice(recordIndex, recordIndex + rowsPerPage);
        const renderRows = pageRecords.length > 0 ? pageRecords.length : 1;
        const tableHeight = headerHeight + renderRows * rowHeight;
        doc.font(resources.boldFont)
            .fontSize(12)
            .fillColor(colors.ink)
            .text(title, x, y - 26, { width });
        drawTableFrame(doc, x, y, width, tableHeight, renderRows, resources);
        if (pageRecords.length === 0) {
            const emptyY = y + headerHeight;
            doc.font(resources.regularFont)
                .fontSize(10)
                .fillColor(colors.muted)
                .text("No records included in this statement.", x + 14, emptyY + 11, {
                width: width - 28,
                align: "center"
            });
        }
        else {
            pageRecords.forEach((record, index) => {
                const rowY = y + headerHeight + index * rowHeight;
                drawRecordRow(doc, resources, {
                    x,
                    y: rowY,
                    width,
                    rowHeight,
                    record,
                    isNew: Boolean(input.newRecordId && record.id === input.newRecordId)
                });
            });
        }
        drawTableGrid(doc, x, y, width, tableHeight, renderRows);
        y += tableHeight + 18;
        recordIndex += pageRecords.length;
        isFirstPage = false;
    }
    const totals = summarize(input.records);
    const tone = toneForSignedAmount(totals.recipientNetAmount);
    const netLabel = totals.recipientNetAmount > 0
        ? `${input.ownerName} owes ${input.recipientName}`
        : totals.recipientNetAmount < 0
            ? `${input.recipientName} owes ${input.ownerName}`
            : "Open account is settled";
    if (y > page.height - 110) {
        doc.addPage({ size: "A4", margin: 0 });
        drawBackground(doc);
        drawFooter(doc, resources);
        y = 68;
    }
    doc.roundedRect(x, y, width, 54, 18).fillAndStroke("#171B28", tone.accent);
    doc.font(resources.boldFont)
        .fontSize(12)
        .fillColor(colors.ink)
        .text(`Net: ${netLabel}`, x + 16, y + 14, { width: width - 180 });
    doc.font(resources.boldFont)
        .fontSize(18)
        .fillColor(tone.accent)
        .text(formatSignedCurrency(totals.recipientNetAmount, totals.currency), x + width - 168, y + 14, {
        width: 148,
        align: "right"
    });
}
function drawTableFrame(doc, x, y, width, height, rowCount, resources) {
    const headerHeight = 30;
    doc.roundedRect(x, y, width, height, 18).fillAndStroke(colors.panel, colors.line);
    doc.rect(x + 1, y + 1, width - 2, headerHeight - 1).fill(colors.tableHeader);
    let cursorX = x;
    tableColumns.forEach((column) => {
        const textX = cursorX + 10;
        const labelWidth = column.width - 20;
        doc.font(resources.boldFont)
            .fontSize(8)
            .fillColor(colors.faint)
            .text(column.label, textX, y + 10, {
            width: labelWidth,
            align: column.align ?? "left",
            characterSpacing: 1.1
        });
        cursorX += column.width;
    });
}
function drawTableGrid(doc, x, y, width, height, rowCount) {
    const headerHeight = 30;
    const rowHeight = 34;
    let cursorX = x;
    tableColumns.forEach((column, index) => {
        if (index > 0) {
            doc.moveTo(cursorX, y).lineTo(cursorX, y + height).strokeColor(colors.line).lineWidth(1).stroke();
        }
        cursorX += column.width;
    });
    for (let index = 0; index <= rowCount; index += 1) {
        const lineY = y + headerHeight + index * rowHeight;
        if (lineY >= y + height) {
            continue;
        }
        doc.moveTo(x, lineY).lineTo(x + width, lineY).strokeColor(colors.line).lineWidth(1).stroke();
    }
}
function drawRecordRow(doc, resources, input) {
    const view = toRecipientRecordView(input.record);
    const statusLabel = input.isNew ? "NEW" : input.record.status.toUpperCase();
    const statusColor = input.isNew
        ? colors.gold
        : input.record.status === "open"
            ? colors.success
            : colors.faint;
    doc.rect(input.x + 1, input.y + 1, input.width - 2, input.rowHeight - 2).fill(view.tint);
    if (input.isNew) {
        doc.rect(input.x + 1, input.y + 1, 5, input.rowHeight - 2).fill(colors.gold);
    }
    let cursorX = input.x;
    tableColumns.forEach((column) => {
        const cellX = cursorX + 10;
        const cellWidth = column.width - 20;
        switch (column.key) {
            case "date":
                doc.font(resources.regularFont)
                    .fontSize(9)
                    .fillColor(colors.muted)
                    .text(formatShortDate(input.record.occurredAt), cellX, input.y + 12, {
                    width: cellWidth
                });
                break;
            case "type":
                doc.font(resources.boldFont)
                    .fontSize(9)
                    .fillColor(view.accent)
                    .text(view.typeLabel, cellX, input.y + 12, {
                    width: cellWidth
                });
                break;
            case "purpose":
                doc.font(resources.regularFont)
                    .fontSize(9)
                    .fillColor(colors.ink)
                    .text(truncate(input.record.purpose, 48), cellX, input.y + 8, {
                    width: cellWidth,
                    height: 18,
                    ellipsis: true
                });
                break;
            case "amount":
                doc.font(resources.boldFont)
                    .fontSize(9)
                    .fillColor(view.accent)
                    .text(view.amountLabel, cellX, input.y + 12, {
                    width: cellWidth,
                    align: "right"
                });
                break;
            case "status":
                doc.font(resources.boldFont)
                    .fontSize(8)
                    .fillColor(statusColor)
                    .text(statusLabel, cellX, input.y + 13, {
                    width: cellWidth,
                    align: "center"
                });
                break;
        }
        cursorX += column.width;
    });
}
function drawFooter(doc, resources) {
    doc.font(resources.regularFont)
        .fontSize(9)
        .fillColor(colors.faint)
        .text("Generated by Aeon Finance. Confirm balances directly before settlement.", page.margin, page.height - 44, {
        width: page.width - page.margin * 2,
        align: "center"
    });
}
function summarize(records) {
    const currency = records[0]?.currency ?? "INR";
    const recipientLentTotal = records
        .filter((record) => record.direction === "i_owe")
        .reduce((total, record) => total + Number(record.amount), 0);
    const recipientBorrowedTotal = records
        .filter((record) => record.direction === "owed_to_me")
        .reduce((total, record) => total + Number(record.amount), 0);
    return {
        currency,
        recipientLentTotal,
        recipientBorrowedTotal,
        recipientNetAmount: recipientLentTotal - recipientBorrowedTotal
    };
}
function toRecipientRecordView(record) {
    const numericAmount = Number(record.amount);
    const isRecipientLend = record.direction === "i_owe";
    const signedAmount = isRecipientLend ? numericAmount : numericAmount * -1;
    const tone = toneForSignedAmount(signedAmount);
    return {
        typeLabel: isRecipientLend ? "Lend" : "Borrow",
        signedAmount,
        amountLabel: formatSignedCurrency(signedAmount, record.currency),
        accent: tone.accent,
        tint: tone.tint
    };
}
function toneForSignedAmount(amount) {
    if (amount > 0) {
        return {
            accent: colors.success,
            tint: colors.successSoft
        };
    }
    if (amount < 0) {
        return {
            accent: colors.danger,
            tint: colors.dangerSoft
        };
    }
    return {
        accent: colors.gold,
        tint: colors.panelSoft
    };
}
function resolvePdfResources(doc) {
    const regularFontPath = resolveExistingPath([
        join(requireDir, "../../assets/fonts/NotoSans-Regular.ttf"),
        join(requireDir, "../../assets/fonts/DejaVuSans.ttf"),
        "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
        "/usr/share/fonts/truetype/liberation2/LiberationSans-Regular.ttf",
        "/usr/share/fonts/truetype/noto/NotoSans-Regular.ttf",
        "C:\\Windows\\Fonts\\arial.ttf",
        "C:\\Windows\\Fonts\\segoeui.ttf",
        "C:\\Windows\\Fonts\\verdana.ttf",
        "C:\\Windows\\Fonts\\tahoma.ttf"
    ]);
    const boldFontPath = resolveExistingPath([
        join(requireDir, "../../assets/fonts/NotoSans-Bold.ttf"),
        join(requireDir, "../../assets/fonts/DejaVuSans-Bold.ttf"),
        "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
        "/usr/share/fonts/truetype/liberation2/LiberationSans-Bold.ttf",
        "/usr/share/fonts/truetype/noto/NotoSans-Bold.ttf",
        "C:\\Windows\\Fonts\\arialbd.ttf",
        "C:\\Windows\\Fonts\\segoeuib.ttf",
        "C:\\Windows\\Fonts\\verdanab.ttf",
        "C:\\Windows\\Fonts\\tahomabd.ttf"
    ]);
    const logoPath = resolveExistingPath([
        join(requireDir, "../../../app/src/assets/aeon-logo.png"),
        join(requireDir, "../../../app/src/main/res/drawable/aeon_logo.png"),
        join(process.cwd(), "../app/src/assets/aeon-logo.png"),
        join(process.cwd(), "../app/src/main/res/drawable/aeon_logo.png")
    ]);
    const regularFont = regularFontPath ? "AeonPdfRegular" : "Helvetica";
    const boldFont = boldFontPath || regularFontPath ? "AeonPdfBold" : "Helvetica-Bold";
    if (regularFontPath) {
        doc.registerFont(regularFont, regularFontPath);
    }
    if (boldFontPath) {
        doc.registerFont(boldFont, boldFontPath);
    }
    else if (regularFontPath) {
        doc.registerFont(boldFont, regularFontPath);
    }
    return {
        logoPath,
        regularFont,
        boldFont
    };
}
function resolveExistingPath(candidates) {
    return candidates.find((candidate) => existsSync(candidate));
}
function formatCurrency(amount, currency) {
    try {
        return new Intl.NumberFormat("en-IN", {
            style: "currency",
            currency,
            currencyDisplay: "symbol",
            maximumFractionDigits: 2
        }).format(amount);
    }
    catch {
        return `${resolveCurrencySymbol(currency)}${amount.toFixed(2)}`;
    }
}
function formatSignedCurrency(amount, currency) {
    if (amount > 0) {
        return `+${formatCurrency(Math.abs(amount), currency)}`;
    }
    if (amount < 0) {
        return `-${formatCurrency(Math.abs(amount), currency)}`;
    }
    return formatCurrency(0, currency);
}
function resolveCurrencySymbol(currency) {
    switch (currency.toUpperCase()) {
        case "INR":
            return "₹";
        case "USD":
            return "$";
        case "EUR":
            return "€";
        case "GBP":
            return "£";
        default:
            return `${currency.toUpperCase()} `;
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
