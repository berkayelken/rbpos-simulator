package io.github.berkayelken.reputation.based.pos.simulator.excel;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

import io.github.berkayelken.reputation.based.pos.simulator.domain.CoinAge;
import io.github.berkayelken.reputation.based.pos.simulator.domain.request.StakingSimulationRequest;
import io.github.berkayelken.reputation.based.pos.simulator.domain.response.StakingSimulationResponse;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public final class ExcelWriter {

	public static void writeToExcelFile(Map<CoinAge, StakingSimulationResponse> stakingScenarios,
			StakingSimulationRequest request) {

		XSSFWorkbook workbook = new XSSFWorkbook();
		XSSFSheet sheet = workbook.createSheet("Staking Books");

		Row caption = sheet.createRow(0);
		caption.createCell(1, CellType.STRING).setCellValue("Pure PoS");
		caption.createCell(2, CellType.STRING).setCellValue("RBPoS");

		StakingSimulationResponse pure = stakingScenarios.get(CoinAge.PURE);
		StakingSimulationResponse reputation = stakingScenarios.get(CoinAge.REPUTATION);

		for (int rowIndexer = 1; rowIndexer < pure.getFinalChainSize(); rowIndexer++) {
			Row row = sheet.createRow(rowIndexer);

			int index = rowIndexer - 1;
			row.createCell(0, CellType.NUMERIC).setCellValue(index);
			row.createCell(1, CellType.NUMERIC).setCellValue(pure.getMajorValidatorCoinAgeRatios().get(index).replace(".", ","));
			if (reputation.getMajorValidatorCoinAgeRatios().size() <= index) {
				row.createCell(2, CellType.NUMERIC).setCellValue(0.0d);
			} else {
				row.createCell(2, CellType.NUMERIC)
						.setCellValue(reputation.getMajorValidatorCoinAgeRatios().get(index).replace(".", ","));
			}
		}
		try (FileOutputStream outputStream = new FileOutputStream(request.getExcelFileName())) {
			workbook.write(outputStream);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

}
