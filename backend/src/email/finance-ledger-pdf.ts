import { existsSync } from "node:fs";
import { createRequire } from "node:module";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import type PDFKit from "pdfkit";

const require = createRequire(import.meta.url);
const PDFDocument = require("pdfkit") as {
  new(options?: PDFKit.PDFDocumentOptions): PDFKit.PDFDocument;
};

export type FinanceLedgerStatementRecord = {
  id: string;
  direction: "owed_to_me" | "i_owe";
  purpose: string;
  note: string | null;
  amount: string;
  currency: string;
  status: "open" | "settled";
  occurredAt: string;
  createdAt: string;
};

export type FinanceLedgerStatementPdfInput = {
  ownerName: string;
  ownerEmail?: string | undefined;
  recipientName: string;
  newRecordId?: string | undefined;
  records: FinanceLedgerStatementRecord[];
  statementTitle?: string | undefined;
  tableTitle?: string | undefined;
  letterMessage?: string | undefined;
};

type LedgerTotals = {
  currency: string;
  recipientLentTotal: number;
  recipientBorrowedTotal: number;
  recipientNetAmount: number;
  openCount: number;
};

type PdfResources = {
  appLogoPath: string | undefined;
  regularFont: string;
  boldFont: string;
};

type RecipientRecordView = {
  typeLabel: "Lend" | "Borrow";
  signedAmount: number;
  absoluteAmountLabel: string;
  accent: string;
  soft: string;
};

type StatementState = {
  label: "New" | "Unpaid" | "Paid";
  accent: string;
  soft: string;
};

type TableColumn = {
  key: "date" | "type" | "purpose" | "amount" | "status";
  width: number;
  align?: "left" | "center" | "right";
  label: string;
};

type CardMetric = {
  x: number;
  y: number;
  width: number;
  tone: "violet" | "green" | "gold";
  icon: "record" | "wallet";
  title: string;
  value: string;
  caption: string;
};

const requireDir = dirname(fileURLToPath(import.meta.url));

const page = {
  width: 595.28,
  height: 841.89,
  margin: 30
};

const colors = {
  paper: "#FFFFFF",
  paperEdge: "#F7F7FB",
  border: "#E5E7EF",
  borderStrong: "#D6DAE5",
  ink: "#101B39",
  text: "#1F2937",
  muted: "#556274",
  faint: "#8B94A7",
  gold: "#D4A12D",
  goldSoft: "#FFF7E6",
  goldTint: "#FFF9EF",
  green: "#17A560",
  greenSoft: "#ECFAF2",
  violet: "#7A59F4",
  violetSoft: "#F3EEFF",
  red: "#E15656",
  redSoft: "#FEF1F1",
  blue: "#2F80ED",
  blueSoft: "#EEF6FF",
  shadow: "#EEF1F8",
  footerDot: "#D9D8FF"
};

const tableColumns: TableColumn[] = [
  { key: "date", label: "DATE", width: 128 },
  { key: "type", label: "TYPE", width: 92 },
  { key: "purpose", label: "PURPOSE", width: 165 },
  { key: "amount", label: "AMOUNT", width: 88, align: "right" },
  { key: "status", label: "STATE", width: 62, align: "center" }
];

export async function buildFinanceLedgerStatementPdf(
  input: FinanceLedgerStatementPdfInput
): Promise<Buffer> {
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

  const chunks: Buffer[] = [];
  const resources = resolvePdfResources(doc);
  const done = new Promise<Buffer>((resolve, reject) => {
    doc.on("data", (chunk: Buffer | string) => {
      chunks.push(Buffer.isBuffer(chunk) ? chunk : Buffer.from(chunk));
    });
    doc.on("end", () => resolve(Buffer.concat(chunks)));
    doc.on("error", reject);
  });

  drawPageBackground(doc);
  const headerBottom = drawHeader(doc, input, resources);
  const summaryBottom = drawSummary(doc, input, resources, headerBottom + 24);
  const letterBottom = drawLetter(doc, input, resources, summaryBottom + 14);
  const recordsBottom = drawRecordsTable(doc, input, resources, letterBottom + 26);
  drawNetSummary(doc, input, resources, recordsBottom + 18);
  drawFooter(doc, resources);

  doc.end();
  return done;
}

function drawPageBackground(doc: PDFKit.PDFDocument): void {
  doc.rect(0, 0, page.width, page.height).fill(colors.paper);
  doc.save();
  doc.lineWidth(1);
  doc.strokeColor(colors.border);
  doc.roundedRect(12, 12, page.width - 24, page.height - 24, 18).stroke();
  doc.fillColor(colors.gold).opacity(0.03).circle(page.width - 66, 70, 82).fill();
  doc.fillColor(colors.violet).opacity(0.025).circle(92, page.height - 110, 96).fill();
  doc.restore();
}

function drawHeader(
  doc: PDFKit.PDFDocument,
  input: FinanceLedgerStatementPdfInput,
  resources: PdfResources
): number {
  const contentWidth = page.width - page.margin * 2;

  drawStatementWordmark(doc, resources, page.margin, 44);
  drawAppBrandSeal(doc, resources, page.width - page.margin - 104, 38);

  doc.font(resources.boldFont)
    .fontSize(32)
    .fillColor(colors.ink)
    .text(input.statementTitle ?? "Open Ledger Account", page.margin, 116, {
      width: 360,
      lineGap: 3
    });

  doc.roundedRect(page.margin, 184, 44, 3, 1.5).fill(colors.gold);

  drawPreparedForBlock(doc, input, resources, page.margin, 204);
  drawStatementDateCard(doc, resources, page.margin + contentWidth - 186, 216);

  return 258;
}

function drawStatementWordmark(
  doc: PDFKit.PDFDocument,
  resources: PdfResources,
  x: number,
  y: number
): void {
  drawStatementBadge(doc, resources, x, y, 36);

  doc.font(resources.boldFont)
    .fontSize(17)
    .fillColor(colors.ink)
    .text("AEON FINANCE", x + 50, y + 6, {
      width: 210,
      characterSpacing: 2
    });

  doc.font(resources.boldFont)
    .fontSize(10.5)
    .fillColor(colors.gold)
    .text("STATEMENT", x + 50, y + 28, {
      width: 160,
      characterSpacing: 3.2
    });
}

function drawStatementBadge(
  doc: PDFKit.PDFDocument,
  resources: PdfResources,
  x: number,
  y: number,
  size: number
): void {
  doc.save();
  doc.roundedRect(x, y, size, size, 8).lineWidth(1.2).fillAndStroke(colors.paper, colors.gold);
  doc.font(resources.boldFont)
    .fontSize(25)
    .fillColor(colors.gold)
    .text("A", x, y + 5, {
      width: size,
      align: "center"
    });
  doc.restore();
}

function drawAppBrandSeal(
  doc: PDFKit.PDFDocument,
  resources: PdfResources,
  x: number,
  y: number
): void {
  const circleSize = 72;
  const circleX = x + 20;
  const circleY = y;

  doc.save();
  doc.fillColor(colors.paper).circle(circleX + circleSize / 2, circleY + circleSize / 2, circleSize / 2).fill();
  doc.lineWidth(1);
  doc.strokeColor(colors.borderStrong);
  doc.circle(circleX + circleSize / 2, circleY + circleSize / 2, circleSize / 2).stroke();

  if (resources.appLogoPath) {
    try {
      doc.image(resources.appLogoPath, circleX + 15, circleY + 15, {
        fit: [42, 42],
        align: "center",
        valign: "center"
      });
    } catch {
      drawFallbackBrandSeal(doc, resources, circleX, circleY, circleSize);
    }
  } else {
    drawFallbackBrandSeal(doc, resources, circleX, circleY, circleSize);
  }

  doc.font(resources.boldFont)
    .fontSize(10.5)
    .fillColor(colors.muted)
    .text("AEON", x, y + 86, {
      width: 112,
      align: "center",
      characterSpacing: 3.1
    });
  doc.restore();
}

function drawFallbackBrandSeal(
  doc: PDFKit.PDFDocument,
  resources: PdfResources,
  x: number,
  y: number,
  size: number
): void {
  doc.font(resources.boldFont)
    .fontSize(28)
    .fillColor(colors.blue)
    .text("A", x, y + 20, {
      width: size,
      align: "center"
    });
}

function drawPreparedForBlock(
  doc: PDFKit.PDFDocument,
  input: FinanceLedgerStatementPdfInput,
  resources: PdfResources,
  x: number,
  y: number
): void {
  drawIconCircle(doc, x, y + 12, 20, colors.goldSoft, colors.gold, "user");

  doc.font(resources.regularFont)
    .fontSize(11)
    .fillColor(colors.muted)
    .text("Prepared for", x + 42, y + 2, {
      width: 160
    });

  doc.font(resources.boldFont)
    .fontSize(18)
    .fillColor(colors.ink)
    .text(input.recipientName, x + 42, y + 22, {
      width: 220
    });

  const ownerRowY = y + 58;
  drawInlineMetaItem(doc, resources, x, ownerRowY, "user", input.ownerName, 210);

  if (input.ownerEmail) {
    const dividerX = x + 212;
    doc.moveTo(dividerX, ownerRowY + 2).lineTo(dividerX, ownerRowY + 20).strokeColor(colors.borderStrong).lineWidth(1).stroke();
    drawInlineMetaItem(doc, resources, dividerX + 14, ownerRowY, "mail", input.ownerEmail, 170);
  }
}

function drawInlineMetaItem(
  doc: PDFKit.PDFDocument,
  resources: PdfResources,
  x: number,
  y: number,
  icon: "user" | "mail",
  text: string,
  width: number
): void {
  drawSmallLineIcon(doc, x, y + 4, 15, colors.muted, icon);
  doc.font(resources.regularFont)
    .fontSize(10.5)
    .fillColor(colors.muted)
    .text(text, x + 24, y + 1, {
      width
    });
}

function drawStatementDateCard(
  doc: PDFKit.PDFDocument,
  resources: PdfResources,
  x: number,
  y: number
): void {
  doc.save();
  drawSoftShadow(doc, x, y, 156, 56, 18);
  doc.roundedRect(x, y, 156, 56, 18).fillAndStroke(colors.paper, colors.border);
  drawSmallLineIcon(doc, x + 18, y + 18, 18, colors.gold, "calendar");
  doc.font(resources.regularFont)
    .fontSize(10.5)
    .fillColor(colors.muted)
    .text("Statement Date", x + 48, y + 16, {
      width: 92
    });
  doc.font(resources.boldFont)
    .fontSize(14)
    .fillColor(colors.ink)
    .text(formatStatementDate(new Date().toISOString()), x + 48, y + 28, {
      width: 92
    });
  doc.restore();
}

function drawSummary(
  doc: PDFKit.PDFDocument,
  input: FinanceLedgerStatementPdfInput,
  resources: PdfResources,
  startY: number
): number {
  const totals = summarize(input.records);
  const primaryRecord = input.newRecordId
    ? input.records.find((record) => record.id === input.newRecordId)
    : input.records.at(-1);
  const primaryView = primaryRecord ? toRecipientRecordView(primaryRecord) : null;
  const openTone = toneForSignedAmount(totals.recipientNetAmount);
  const newEntryTitle = input.newRecordId ? "NEW RECORD" : "LATEST RECORD";
  const currentTotalTitle = "UNPAID TOTAL";

  drawMetricCard(doc, resources, {
    x: page.margin,
    y: startY,
    width: 250,
    tone: "violet",
    icon: "record",
    title: newEntryTitle,
    value: primaryRecord
      ? formatCurrency(Number(primaryRecord.amount), primaryRecord.currency)
      : formatCurrency(0, totals.currency),
    caption: primaryRecord ? truncate(primaryRecord.purpose, 42) : "No record available"
  });

  drawMetricCard(doc, resources, {
    x: page.margin + 262,
    y: startY,
    width: 273,
    tone: openTone.accent === colors.green ? "green" : openTone.accent === colors.gold ? "gold" : "violet",
    icon: "wallet",
    title: currentTotalTitle,
    value: formatCurrency(Math.abs(totals.recipientNetAmount), totals.currency),
    caption: buildTotalCaption(totals)
  });

  return startY + 96;
}

function buildTotalCaption(totals: LedgerTotals): string {
  if (totals.recipientNetAmount > 0) {
    return `You receive | ${totals.openCount} unpaid record${totals.openCount === 1 ? "" : "s"}`;
  }

  if (totals.recipientNetAmount < 0) {
    return `You owe | ${totals.openCount} unpaid record${totals.openCount === 1 ? "" : "s"}`;
  }

  return `${totals.openCount} record${totals.openCount === 1 ? "" : "s"} balanced`;
}

function drawMetricCard(
  doc: PDFKit.PDFDocument,
  resources: PdfResources,
  metric: CardMetric
): void {
  const tone = resolveCardTone(metric.tone);
  const iconBoxSize = 64;

  doc.save();
  drawSoftShadow(doc, metric.x, metric.y, metric.width, 88, 22);
  doc.roundedRect(metric.x, metric.y, metric.width, 88, 22).fillAndStroke(colors.paper, tone.border);
  doc.roundedRect(metric.x + 18, metric.y + 18, iconBoxSize, iconBoxSize, 18).fillAndStroke(tone.soft, tone.border);
  drawLargeCardIcon(doc, metric.x + 18, metric.y + 18, iconBoxSize, tone.accent, metric.icon);

  doc.save();
  doc.moveTo(metric.x + 102, metric.y + 18)
    .lineTo(metric.x + 102, metric.y + 70)
    .dash(2, { space: 3 })
    .strokeColor(colors.borderStrong)
    .lineWidth(1)
    .stroke();
  doc.undash();
  doc.restore();

  doc.font(resources.boldFont)
    .fontSize(10)
    .fillColor(tone.accent)
    .text(metric.title, metric.x + 124, metric.y + 20, {
      width: metric.width - 144,
      characterSpacing: 1.4
    });

  doc.font(resources.boldFont)
    .fontSize(23)
    .fillColor(colors.ink)
    .text(metric.value, metric.x + 124, metric.y + 36, {
      width: metric.width - 144
    });

  doc.font(resources.regularFont)
    .fontSize(10.5)
    .fillColor(colors.muted)
    .text(metric.caption, metric.x + 124, metric.y + 64, {
      width: metric.width - 144,
      ellipsis: true
    });
  doc.restore();
}

function resolveCardTone(tone: CardMetric["tone"]): { accent: string; soft: string; border: string } {
  switch (tone) {
    case "green":
      return {
        accent: colors.green,
        soft: colors.greenSoft,
        border: "#D8F1E2"
      };
    case "gold":
      return {
        accent: colors.gold,
        soft: colors.goldSoft,
        border: "#F1DFC0"
      };
    default:
      return {
        accent: colors.violet,
        soft: colors.violetSoft,
        border: "#E2DAFF"
      };
  }
}

function drawLetter(
  doc: PDFKit.PDFDocument,
  input: FinanceLedgerStatementPdfInput,
  resources: PdfResources,
  startY: number
): number {
  if (!input.letterMessage?.trim()) {
    return startY;
  }

  const height = 52;
  const width = page.width - page.margin * 2;

  doc.save();
  drawSoftShadow(doc, page.margin, startY, width, height, 18);
  doc.roundedRect(page.margin, startY, width, height, 18).fillAndStroke(colors.paper, colors.border);
  doc.font(resources.boldFont)
    .fontSize(9)
    .fillColor(colors.gold)
    .text(`LETTER FROM ${input.ownerName.toUpperCase()}`, page.margin + 16, startY + 12, {
      width: width - 32,
      characterSpacing: 1.2
    });
  doc.font(resources.regularFont)
    .fontSize(10.5)
    .fillColor(colors.text)
    .text(truncate(input.letterMessage, 190), page.margin + 16, startY + 29, {
      width: width - 32,
      ellipsis: true
    });
  doc.restore();

  return startY + height;
}

function drawRecordsTable(
  doc: PDFKit.PDFDocument,
  input: FinanceLedgerStatementPdfInput,
  resources: PdfResources,
  startY: number
): number {
  let y = startY;
  let index = 0;
  let pageIndex = 0;
  const tableWidth = page.width - page.margin * 2;
  const rowHeight = 46;
  const headerHeight = 38;
  const sectionTitle = input.tableTitle ?? "Open Records";

  while (index < input.records.length || (input.records.length === 0 && pageIndex === 0)) {
    if (pageIndex > 0) {
      doc.addPage({ size: "A4", margin: 0 });
      drawPageBackground(doc);
      drawContinuationHeader(doc, resources, sectionTitle);
      y = 104;
    } else {
      drawSectionHeading(doc, resources, page.margin, y - 26, sectionTitle);
    }

    const availableHeight = page.height - y - 152;
    const rowsPerPage = Math.max(1, Math.floor((availableHeight - headerHeight) / rowHeight));
    const pageRecords = input.records.slice(index, index + rowsPerPage);
    const renderRows = Math.max(pageRecords.length, 1);
    const tableHeight = headerHeight + renderRows * rowHeight;

    drawTableFrame(doc, resources, page.margin, y, tableWidth, tableHeight, renderRows);

    if (pageRecords.length === 0) {
      doc.font(resources.regularFont)
        .fontSize(10.5)
        .fillColor(colors.muted)
        .text("No records included in this statement.", page.margin, y + headerHeight + 16, {
          width: tableWidth,
          align: "center"
        });
      index = input.records.length;
    } else {
      pageRecords.forEach((record, rowIndex) => {
        drawRecordRow(doc, resources, {
          x: page.margin,
          y: y + headerHeight + rowIndex * rowHeight,
          rowHeight,
          record,
          isNew: Boolean(input.newRecordId && record.id === input.newRecordId)
        });
      });
      index += pageRecords.length;
    }

    drawTableGrid(doc, page.margin, y, tableWidth, tableHeight, renderRows, rowHeight);

    y += tableHeight + 26;
    pageIndex += 1;
  }

  return y;
}

function drawSectionHeading(
  doc: PDFKit.PDFDocument,
  resources: PdfResources,
  x: number,
  y: number,
  title: string
): void {
  drawSmallLineIcon(doc, x, y + 3, 18, colors.gold, "list");
  doc.font(resources.boldFont)
    .fontSize(14.5)
    .fillColor(colors.ink)
    .text(title, x + 28, y, {
      width: 260
    });
}

function drawContinuationHeader(
  doc: PDFKit.PDFDocument,
  resources: PdfResources,
  title: string
): void {
  drawStatementWordmark(doc, resources, page.margin, 30);
  doc.font(resources.boldFont)
    .fontSize(18)
    .fillColor(colors.ink)
    .text(title, page.margin, 72, {
      width: 250
    });
}

function drawTableFrame(
  doc: PDFKit.PDFDocument,
  resources: PdfResources,
  x: number,
  y: number,
  width: number,
  height: number,
  rowCount: number
): void {
  const headerHeight = 38;

  doc.save();
  drawSoftShadow(doc, x, y, width, height, 20);
  doc.roundedRect(x, y, width, height, 20).fillAndStroke(colors.paper, colors.border);
  doc.roundedRect(x + 1, y + 1, width - 2, headerHeight - 1, 20).fill(colors.paperEdge);

  let cursorX = x;
  tableColumns.forEach((column) => {
    doc.font(resources.boldFont)
      .fontSize(9)
      .fillColor(colors.faint)
      .text(column.label, cursorX + 12, y + 13, {
        width: column.width - 24,
        align: column.align ?? "left",
        characterSpacing: 1.2
      });
    cursorX += column.width;
  });
  doc.restore();
}

function drawTableGrid(
  doc: PDFKit.PDFDocument,
  x: number,
  y: number,
  width: number,
  height: number,
  rowCount: number,
  rowHeight: number
): void {
  const headerHeight = 38;
  let cursorX = x;

  tableColumns.forEach((column, index) => {
    if (index > 0) {
      doc.moveTo(cursorX, y + 10)
        .lineTo(cursorX, y + height - 10)
        .strokeColor(colors.border)
        .lineWidth(1)
        .stroke();
    }
    cursorX += column.width;
  });

  for (let rowIndex = 0; rowIndex <= rowCount; rowIndex += 1) {
    const lineY = y + headerHeight + rowIndex * rowHeight;
    if (lineY >= y + height) {
      continue;
    }
    doc.moveTo(x, lineY)
      .lineTo(x + width, lineY)
      .strokeColor(colors.border)
      .lineWidth(1)
      .stroke();
  }
}

function drawRecordRow(
  doc: PDFKit.PDFDocument,
  resources: PdfResources,
  input: {
    x: number;
    y: number;
    rowHeight: number;
    record: FinanceLedgerStatementRecord;
    isNew: boolean;
  }
): void {
  const view = toRecipientRecordView(input.record);
  const state = resolveStatementState(input.record, input.isNew);
  let cursorX = input.x;

  tableColumns.forEach((column) => {
    const cellX = cursorX + 12;
    const cellWidth = column.width - 24;

    switch (column.key) {
      case "date":
        drawDateCell(doc, resources, cellX, input.y + 10, input.record.occurredAt, input.record.direction);
        break;
      case "type":
        drawPillChip(doc, resources, cellX, input.y + 12, view.typeLabel, view.soft, view.accent);
        break;
      case "purpose":
        doc.font(resources.regularFont)
          .fontSize(10.5)
          .fillColor(colors.text)
          .text(truncate(input.record.purpose, 34), cellX, input.y + 15, {
            width: cellWidth,
            ellipsis: true
          });
        break;
      case "amount":
        doc.font(resources.boldFont)
          .fontSize(11.5)
          .fillColor(colors.ink)
          .text(view.absoluteAmountLabel, cellX, input.y + 15, {
            width: cellWidth,
            align: "right"
          });
        break;
      case "status":
        drawPillChip(doc, resources, cellX - 2, input.y + 12, state.label, state.soft, state.accent, 58);
        break;
    }

    cursorX += column.width;
  });
}

function drawDateCell(
  doc: PDFKit.PDFDocument,
  resources: PdfResources,
  x: number,
  y: number,
  occurredAt: string,
  direction: FinanceLedgerStatementRecord["direction"]
): void {
  const tone = direction === "i_owe"
    ? { soft: colors.violetSoft, accent: colors.violet }
    : { soft: colors.goldSoft, accent: colors.gold };

  doc.roundedRect(x, y - 3, 26, 26, 13).fillAndStroke(tone.soft, tone.soft);
  drawSmallLineIcon(doc, x + 6, y + 3, 14, tone.accent, "calendar");

  doc.font(resources.regularFont)
    .fontSize(10.5)
    .fillColor(colors.text)
    .text(formatLongDate(occurredAt), x + 36, y, {
      width: 92
    });
}

function resolveStatementState(
  record: FinanceLedgerStatementRecord,
  isNew: boolean
): StatementState {
  if (isNew) {
    return {
      label: "New",
      accent: colors.gold,
      soft: colors.goldSoft
    };
  }

  if (record.status === "settled") {
    return {
      label: "Paid",
      accent: colors.violet,
      soft: colors.violetSoft
    };
  }

  return {
    label: "Unpaid",
    accent: colors.green,
    soft: colors.greenSoft
  };
}

function drawPillChip(
  doc: PDFKit.PDFDocument,
  resources: PdfResources,
  x: number,
  y: number,
  text: string,
  fill: string,
  accent: string,
  minWidth = 66
): void {
  doc.font(resources.boldFont).fontSize(10);
  const measuredWidth = Math.max(minWidth, Math.ceil(doc.widthOfString(text) + 24));
  doc.roundedRect(x, y, measuredWidth, 22, 11).fillAndStroke(fill, fill);
  doc.font(resources.boldFont)
    .fontSize(10)
    .fillColor(accent)
    .text(text, x, y + 6.5, {
      width: measuredWidth,
      align: "center"
    });
}

function drawNetSummary(
  doc: PDFKit.PDFDocument,
  input: FinanceLedgerStatementPdfInput,
  resources: PdfResources,
  y: number
): number {
  const width = page.width - page.margin * 2;
  const totals = summarize(input.records);
  const tone = toneForSignedAmount(totals.recipientNetAmount);
  const netLabel = totals.recipientNetAmount > 0
    ? `${input.ownerName} owes ${input.recipientName}`
    : totals.recipientNetAmount < 0
      ? `${input.recipientName} owes ${input.ownerName}`
      : "No unpaid balance remains";

  if (y > page.height - 152) {
    doc.addPage({ size: "A4", margin: 0 });
    drawPageBackground(doc);
    drawContinuationHeader(doc, resources, "Statement Summary");
    y = 124;
  }

  doc.save();
  drawSoftShadow(doc, page.margin, y, width, 60, 18);
  doc.roundedRect(page.margin, y, width, 60, 18).fillAndStroke(colors.paper, "#E8D29C");
  drawIconCircle(doc, page.margin + 16, y + 10, 20, colors.goldSoft, colors.gold, "balance");

  doc.font(resources.boldFont)
    .fontSize(12)
    .fillColor(colors.ink)
    .text(`Net: ${netLabel}`, page.margin + 52, y + 20, {
      width: width - 190
    });

  doc.font(resources.boldFont)
    .fontSize(18)
    .fillColor(tone.accent)
    .text(formatSignedCurrency(totals.recipientNetAmount, totals.currency), page.margin + width - 162, y + 17, {
      width: 138,
      align: "right"
    });
  doc.restore();

  return y + 60;
}

function drawFooter(
  doc: PDFKit.PDFDocument,
  resources: PdfResources
): void {
  const footerY = page.height - 108;
  const footerWidth = page.width - page.margin * 2;

  doc.save();
  drawSoftShadow(doc, page.margin, footerY, footerWidth, 62, 18);
  doc.roundedRect(page.margin, footerY, footerWidth, 62, 18).fillAndStroke(colors.paper, colors.border);
  drawIconCircle(doc, page.margin + 18, footerY + 11, 20, colors.violetSoft, colors.violet, "shield");

  doc.font(resources.boldFont)
    .fontSize(12)
    .fillColor(colors.ink)
    .text("Generated by Aeon Finance.", page.margin + 54, footerY + 18, {
      width: 220
    });

  doc.font(resources.regularFont)
    .fontSize(10.5)
    .fillColor(colors.muted)
    .text("Confirm balances directly before settlement.", page.margin + 54, footerY + 36, {
      width: 240
    });

  drawDotPattern(doc, page.margin + footerWidth - 124, footerY + 20, 8, 4, 10, 8);
  doc.restore();

  const ornamentY = page.height - 28;
  doc.moveTo(page.margin, ornamentY)
    .lineTo(page.width / 2 - 16, ornamentY)
    .strokeColor("#E5C772")
    .lineWidth(1.2)
    .stroke();
  doc.moveTo(page.width / 2 + 16, ornamentY)
    .lineTo(page.width - page.margin, ornamentY)
    .strokeColor("#E5C772")
    .lineWidth(1.2)
    .stroke();

  drawBottomOrnament(doc, page.width / 2, ornamentY - 2);
}

function drawSoftShadow(
  doc: PDFKit.PDFDocument,
  x: number,
  y: number,
  width: number,
  height: number,
  radius: number
): void {
  doc.save();
  doc.fillColor(colors.shadow).opacity(0.28);
  doc.roundedRect(x, y + 4, width, height, radius).fill();
  doc.restore();
}

function drawDotPattern(
  doc: PDFKit.PDFDocument,
  x: number,
  y: number,
  columns: number,
  rows: number,
  xGap: number,
  yGap: number
): void {
  doc.save();
  doc.fillColor(colors.footerDot);
  for (let row = 0; row < rows; row += 1) {
    for (let column = 0; column < columns; column += 1) {
      doc.circle(x + column * xGap, y + row * yGap, 1.2).fill();
    }
  }
  doc.restore();
}

function drawBottomOrnament(
  doc: PDFKit.PDFDocument,
  centerX: number,
  centerY: number
): void {
  doc.save();
  doc.strokeColor(colors.gold).lineWidth(1.2);
  doc.moveTo(centerX - 7, centerY).lineTo(centerX + 7, centerY).stroke();
  doc.moveTo(centerX, centerY - 7).lineTo(centerX, centerY + 7).stroke();
  doc.moveTo(centerX - 4.5, centerY - 4.5).lineTo(centerX + 4.5, centerY + 4.5).stroke();
  doc.moveTo(centerX + 4.5, centerY - 4.5).lineTo(centerX - 4.5, centerY + 4.5).stroke();
  doc.restore();
}

function drawIconCircle(
  doc: PDFKit.PDFDocument,
  x: number,
  y: number,
  radius: number,
  fill: string,
  accent: string,
  icon: "user" | "balance" | "shield"
): void {
  const centerX = x + radius;
  const centerY = y + radius;
  doc.save();
  doc.fillColor(fill).circle(centerX, centerY, radius).fill();
  doc.strokeColor(accent).lineWidth(1.4);

  switch (icon) {
    case "user":
      doc.circle(centerX, centerY - 6, 4.4).stroke();
      doc.moveTo(centerX - 8, centerY + 8)
        .bezierCurveTo(centerX - 8, centerY + 1, centerX + 8, centerY + 1, centerX + 8, centerY + 8)
        .stroke();
      break;
    case "balance":
      doc.moveTo(centerX, centerY - 10).lineTo(centerX, centerY + 8).stroke();
      doc.moveTo(centerX - 10, centerY - 6).lineTo(centerX + 10, centerY - 6).stroke();
      doc.moveTo(centerX - 6, centerY - 6).lineTo(centerX - 12, centerY + 2).stroke();
      doc.moveTo(centerX - 6, centerY - 6).lineTo(centerX, centerY + 2).stroke();
      doc.moveTo(centerX + 6, centerY - 6).lineTo(centerX, centerY + 2).stroke();
      doc.moveTo(centerX + 6, centerY - 6).lineTo(centerX + 12, centerY + 2).stroke();
      doc.moveTo(centerX - 12, centerY + 2).lineTo(centerX, centerY + 2).stroke();
      doc.moveTo(centerX, centerY + 2).lineTo(centerX + 12, centerY + 2).stroke();
      doc.moveTo(centerX - 7, centerY + 10).lineTo(centerX + 7, centerY + 10).stroke();
      break;
    case "shield":
      doc.moveTo(centerX, centerY - 11)
        .lineTo(centerX + 9, centerY - 7)
        .lineTo(centerX + 7, centerY + 6)
        .lineTo(centerX, centerY + 11)
        .lineTo(centerX - 7, centerY + 6)
        .lineTo(centerX - 9, centerY - 7)
        .closePath()
        .stroke();
      doc.moveTo(centerX, centerY - 5).lineTo(centerX, centerY + 5).stroke();
      doc.moveTo(centerX - 4, centerY).lineTo(centerX, centerY + 4).lineTo(centerX + 5, centerY - 3).stroke();
      break;
  }
  doc.restore();
}

function drawSmallLineIcon(
  doc: PDFKit.PDFDocument,
  x: number,
  y: number,
  size: number,
  accent: string,
  icon: "user" | "mail" | "calendar" | "list"
): void {
  const right = x + size;
  const bottom = y + size;
  const midX = x + size / 2;
  const midY = y + size / 2;

  doc.save();
  doc.strokeColor(accent).lineWidth(1.4);

  switch (icon) {
    case "user":
      doc.circle(midX, y + 4.8, 2.8).stroke();
      doc.moveTo(x + 2, bottom - 2)
        .bezierCurveTo(x + 2, midY + 1, right - 2, midY + 1, right - 2, bottom - 2)
        .stroke();
      break;
    case "mail":
      doc.roundedRect(x + 1, y + 3, size - 2, size - 7, 2).stroke();
      doc.moveTo(x + 2, y + 5).lineTo(midX, y + 10).lineTo(right - 2, y + 5).stroke();
      break;
    case "calendar":
      doc.roundedRect(x + 1.5, y + 3, size - 3, size - 4, 3).stroke();
      doc.moveTo(x + 1.5, y + 7).lineTo(right - 1.5, y + 7).stroke();
      doc.moveTo(x + 5, y + 1.5).lineTo(x + 5, y + 5).stroke();
      doc.moveTo(right - 5, y + 1.5).lineTo(right - 5, y + 5).stroke();
      break;
    case "list":
      for (let row = 0; row < 3; row += 1) {
        const rowY = y + 4 + row * 5;
        doc.circle(x + 2.5, rowY + 1, 0.8).fillAndStroke(accent, accent);
        doc.moveTo(x + 6, rowY + 1).lineTo(right - 1, rowY + 1).stroke();
      }
      break;
  }

  doc.restore();
}

function drawLargeCardIcon(
  doc: PDFKit.PDFDocument,
  x: number,
  y: number,
  size: number,
  accent: string,
  icon: CardMetric["icon"]
): void {
  doc.save();
  doc.strokeColor(accent).lineWidth(2);

  if (icon === "record") {
    doc.roundedRect(x + 17, y + 13, 24, 34, 6).stroke();
    doc.moveTo(x + 23, y + 21).lineTo(x + 35, y + 21).stroke();
    doc.moveTo(x + 23, y + 28).lineTo(x + 35, y + 28).stroke();
    doc.circle(x + 40, y + 38, 7).stroke();
    doc.moveTo(x + 40, y + 34).lineTo(x + 40, y + 42).stroke();
    doc.moveTo(x + 36, y + 38).lineTo(x + 44, y + 38).stroke();
  } else {
    doc.roundedRect(x + 17, y + 24, 30, 18, 4).stroke();
    doc.moveTo(x + 24, y + 24).lineTo(x + 30, y + 18).lineTo(x + 40, y + 18).lineTo(x + 42, y + 24).stroke();
    doc.moveTo(x + 22, y + 31).lineTo(x + 42, y + 31).stroke();
  }

  doc.restore();
}

function summarize(records: FinanceLedgerStatementRecord[]): LedgerTotals {
  const currency = records[0]?.currency ?? "INR";
  const recipientLentTotal = records
    .filter((record) => record.status === "open" && record.direction === "i_owe")
    .reduce((total, record) => total + Number(record.amount), 0);
  const recipientBorrowedTotal = records
    .filter((record) => record.status === "open" && record.direction === "owed_to_me")
    .reduce((total, record) => total + Number(record.amount), 0);

  return {
    currency,
    recipientLentTotal,
    recipientBorrowedTotal,
    recipientNetAmount: recipientLentTotal - recipientBorrowedTotal,
    openCount: records.filter((record) => record.status === "open").length
  };
}

function toRecipientRecordView(record: FinanceLedgerStatementRecord): RecipientRecordView {
  const numericAmount = Number(record.amount);
  const isRecipientLend = record.direction === "i_owe";
  const signedAmount = isRecipientLend ? numericAmount : numericAmount * -1;
  const tone = toneForSignedAmount(signedAmount);

  return {
    typeLabel: isRecipientLend ? "Lend" : "Borrow",
    signedAmount,
    absoluteAmountLabel: formatCurrency(Math.abs(numericAmount), record.currency),
    accent: tone.accent,
    soft: tone.soft
  };
}

function toneForSignedAmount(amount: number): { accent: string; soft: string } {
  if (amount > 0) {
    return {
      accent: colors.green,
      soft: colors.greenSoft
    };
  }

  if (amount < 0) {
    return {
      accent: colors.violet,
      soft: colors.violetSoft
    };
  }

  return {
    accent: colors.gold,
    soft: colors.goldSoft
  };
}

function resolvePdfResources(doc: PDFKit.PDFDocument): PdfResources {
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
  const appLogoPath = resolveExistingPath([
    join(requireDir, "../../../app/src/main/res/mipmap-xxxhdpi/ic_launcher.png"),
    join(requireDir, "../../../app/src/main/res/mipmap-xxhdpi/ic_launcher.png"),
    join(requireDir, "../../../app/src/assets/aeon-logo.png"),
    join(requireDir, "../../../app/src/main/res/drawable/aeon_logo.png"),
    join(process.cwd(), "../app/src/main/res/mipmap-xxxhdpi/ic_launcher.png"),
    join(process.cwd(), "../app/src/main/res/mipmap-xxhdpi/ic_launcher.png"),
    join(process.cwd(), "../app/src/assets/aeon-logo.png")
  ]);

  const regularFont = regularFontPath ? "AeonPdfRegular" : "Helvetica";
  const boldFont = boldFontPath || regularFontPath ? "AeonPdfBold" : "Helvetica-Bold";

  if (regularFontPath) {
    doc.registerFont(regularFont, regularFontPath);
  }
  if (boldFontPath) {
    doc.registerFont(boldFont, boldFontPath);
  } else if (regularFontPath) {
    doc.registerFont(boldFont, regularFontPath);
  }

  return {
    appLogoPath,
    regularFont,
    boldFont
  };
}

function resolveExistingPath(candidates: string[]): string | undefined {
  return candidates.find((candidate) => existsSync(candidate));
}

function formatCurrency(amount: number, currency: string): string {
  try {
    return new Intl.NumberFormat("en-IN", {
      style: "currency",
      currency,
      currencyDisplay: "symbol",
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    }).format(amount);
  } catch {
    return `${resolveCurrencySymbol(currency)}${amount.toFixed(2)}`;
  }
}

function formatSignedCurrency(amount: number, currency: string): string {
  if (amount > 0) {
    return `+${formatCurrency(Math.abs(amount), currency)}`;
  }

  if (amount < 0) {
    return `-${formatCurrency(Math.abs(amount), currency)}`;
  }

  return formatCurrency(0, currency);
}

function resolveCurrencySymbol(currency: string): string {
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

function formatStatementDate(value: string): string {
  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return value.slice(0, 12);
  }

  return new Intl.DateTimeFormat("en-IN", {
    day: "2-digit",
    month: "long",
    year: "numeric"
  }).format(date);
}

function formatLongDate(value: string): string {
  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return value.slice(0, 11);
  }

  return new Intl.DateTimeFormat("en-IN", {
    day: "2-digit",
    month: "short",
    year: "numeric"
  }).format(date);
}

function truncate(value: string, maxLength: number): string {
  return value.length <= maxLength ? value : `${value.slice(0, Math.max(0, maxLength - 1))}…`;
}
