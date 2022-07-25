function [] = exportError2excel(error)

    % 升序排序
    error = sort(error);
    rows_length = size(error, 1);
    cdfTemp = ones(rows_length, 1);

    for i=1:rows_length
        if i == 1
            cdfTemp(i) = 1 / rows_length;
        else
            cdfTemp(i) = cdfTemp(i-1) + (1 / rows_length);
        end
    end
    % 整合数据
    output_mat = [error cdfTemp];

    % 输出成excel
    xlswrite('out/error_result_mat.xlsx', output_mat);
end