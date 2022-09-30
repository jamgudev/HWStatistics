function [] = exportParams2Excel(theta, mu, sigma)

    mu = [0 mu];
    sigma = [0 sigma];

    output_param = [theta';mu;sigma];

    % 输出成excel
    xlswrite('out/params_mat.xlsx', output_param);

end